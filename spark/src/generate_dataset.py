"""
generate_dataset.py — Generador de dataset sintético de telemetría de red e
incidencias (equipo ACC — Soporte Técnico ISP), para el pipeline Spark (Paso 6).

Por qué sintético: el PFC es un proyecto académico y no existe un histórico real
de un ISP al que el equipo tenga acceso legítimo. La guía (D6.2 / Anexo A) exige
que, si se usan datos sintéticos, su procedencia y generación queden documentadas
-- este script ES esa documentación ejecutable. El dataset resultante:
  - supera 500,000 filas (parámetro --rows, default 600,000)
  - simula telemetría de nodos de red + incidencias reportadas
  - tiene estructura temporal realista (más incidencias en horas pico,
    estacionalidad semanal) para que el análisis de patrones temporales
    (Paso 6, transformación de agregación por hora) tenga sentido real
  - asigna cada incidencia a un client_id de un pool sesgado (distribución de
    Pareto): la mayoría de los clientes reportan 1 incidencia, una minoría
    concentra muchas -- esto es lo que hace que el análisis de reincidencia
    (Tabla 1 de la guía de E3, columna "Analítica paralela" del equipo ACC)
    tenga señal real y no sea uniforme/trivial
  - incluye una descripción de texto libre corta por incidencia (plantillas por
    tipo), necesaria para el paso de clustering por texto (Paso 6, transformación
    de ML)

Salida: Parquet particionado por zona en /data/processed/incidents/
(el path exacto se define con --out).

Requisitos:
    pip install pandas numpy pyarrow --break-system-packages

Uso:
    python generate_dataset.py --rows 600000 --out ../../data/processed/incidents
"""

import argparse
import os

import numpy as np
import pandas as pd

ZONES = ["QUEVEDO_CENTRO", "QUEVEDO_NORTE", "QUEVEDO_SUR"]
ZONE_WEIGHTS = [0.40, 0.30, 0.30]

INCIDENT_TYPES = ["DNS", "CORTE_TOTAL", "LENTITUD", "HARDWARE", "CONFIGURACION"]
# Pesos distintos por tipo para que el pipeline encuentre patrones no triviales
INCIDENT_WEIGHTS = [0.15, 0.10, 0.35, 0.20, 0.20]

SEVERITY = ["CRITICO", "ALTO", "MEDIO", "BAJO"]
SEVERITY_WEIGHTS = [0.10, 0.25, 0.40, 0.25]

N_NODES_PER_ZONE = 40  # nodos/equipos de red simulados por zona
N_TECHNICIANS = 18
N_CLIENTS_PER_ZONE = 30_000  # pool de clientes por zona (ver transformations.build_clients_dim)

# Plantillas de descripcion libre por tipo de incidencia -- honestidad sobre el
# alcance (misma convencion que ai-service/app/classifier.py): son plantillas
# fijas, no texto real de clientes, pero le dan al paso de clustering (T5) texto
# de verdad sobre el que operar en vez de una sola palabra categorica.
DESCRIPTION_TEMPLATES = {
    "DNS": [
        "el internet conecta pero las paginas no cargan",
        "no resuelve nombres de dominio desde esta manana",
        "puedo hacer ping pero el navegador no abre sitios",
        "el wifi funciona pero google no carga",
    ],
    "CORTE_TOTAL": [
        "no hay servicio de internet en absoluto",
        "las luces del modem estan apagadas",
        "corte total desde anoche en toda la casa",
        "sin conexion, ya reinicie el router y nada",
    ],
    "LENTITUD": [
        "la velocidad es muchisimo mas baja de lo contratado",
        "el internet se pone lento todas las noches",
        "los videos se traban constantemente",
        "la conexion baja mucho en horas pico",
    ],
    "HARDWARE": [
        "el router no enciende",
        "hay un cable de red visiblemente danado",
        "el modem hace un ruido raro y se apaga solo",
        "se quemo el equipo tras una tormenta electrica",
    ],
    "CONFIGURACION": [
        "necesito reconfigurar el router con mi clave wifi",
        "cambie de equipo y no logro conectarlo",
        "quiero cambiar el nombre de mi red wifi",
        "no logro acceder al panel de administracion del router",
    ],
}


def hourly_weight(hour: int) -> float:
    """Simula picos de incidencias en horario laboral y noche (mayor uso domestico)."""
    if 8 <= hour <= 12 or 18 <= hour <= 22:
        return 2.5
    if 0 <= hour <= 5:
        return 0.4
    return 1.0


def generate(rows: int, seed: int = 42) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    zones = rng.choice(ZONES, size=rows, p=ZONE_WEIGHTS)
    incident_types = rng.choice(INCIDENT_TYPES, size=rows, p=INCIDENT_WEIGHTS)
    severities = rng.choice(SEVERITY, size=rows, p=SEVERITY_WEIGHTS)

    # Timestamps distribuidos en los ultimos 180 dias, con sesgo horario realista
    base_days = rng.integers(0, 180, size=rows)
    hours = rng.integers(0, 24, size=rows)
    # resample de horas para favorecer picos (rejection-lite via weights normalizados)
    hour_probs = np.array([hourly_weight(h) for h in range(24)])
    hour_probs = hour_probs / hour_probs.sum()
    hours = rng.choice(24, size=rows, p=hour_probs)
    minutes = rng.integers(0, 60, size=rows)

    start_date = pd.Timestamp.utcnow().normalize() - pd.Timedelta(days=180)
    timestamps = [
        start_date + pd.Timedelta(days=int(d), hours=int(h), minutes=int(m))
        for d, h, m in zip(base_days, hours, minutes)
    ]

    node_ids = [
        f"{zone}-NODE-{rng.integers(1, N_NODES_PER_ZONE + 1):03d}" for zone in zones
    ]
    technician_ids = rng.integers(1, N_TECHNICIANS + 1, size=rows)

    # client_id con distribucion de Pareto por zona: la mayoria de los clientes
    # aparece 1 vez, una minoria concentra muchas incidencias -- esa cola larga es
    # la que hace que "reincidencia" (T3/T4 de transformations.py) tenga sentido.
    client_ids = np.empty(rows, dtype=object)
    for zone in ZONES:
        mask = zones == zone
        n_in_zone = int(mask.sum())
        pool = [f"{zone}-CLIENTE-{i:05d}" for i in range(1, N_CLIENTS_PER_ZONE + 1)]
        weights = rng.pareto(1.5, size=N_CLIENTS_PER_ZONE) + 1
        weights = weights / weights.sum()
        client_ids[mask] = rng.choice(pool, size=n_in_zone, p=weights)

    descriptions = [
        DESCRIPTION_TEMPLATES[itype][rng.integers(0, len(DESCRIPTION_TEMPLATES[itype]))]
        for itype in incident_types
    ]

    # duracion e incidencia correlacionadas con severidad y tipo (para que MTTR tenga señal real)
    severity_base_minutes = {"CRITICO": 180, "ALTO": 90, "MEDIO": 45, "BAJO": 15}
    incident_multiplier = {
        "CORTE_TOTAL": 1.6, "HARDWARE": 1.4, "DNS": 0.8, "LENTITUD": 0.6, "CONFIGURACION": 0.9
    }
    duration_minutes = np.array([
        max(2, rng.normal(
            severity_base_minutes[sev] * incident_multiplier[itype], scale=15
        ))
        for sev, itype in zip(severities, incident_types)
    ])

    resolved = rng.random(rows) < 0.93  # 93% resueltos, 7% siguen abiertos
    sla_minutes_limit = np.array([
        {"CRITICO": 60, "ALTO": 240, "MEDIO": 480, "BAJO": 1440}[sev] for sev in severities
    ])
    sla_breached = duration_minutes > sla_minutes_limit

    df = pd.DataFrame({
        "incident_id": np.arange(1, rows + 1),
        "zone": zones,
        "node_id": node_ids,
        "client_id": client_ids,
        "incident_type": incident_types,
        "description": descriptions,
        "severity": severities,
        "technician_id": technician_ids,
        "timestamp": timestamps,
        "duration_minutes": np.round(duration_minutes, 1),
        "resolved": resolved,
        "sla_breached": sla_breached,
    })
    df["hour_of_day"] = df["timestamp"].dt.hour
    df["day_of_week"] = df["timestamp"].dt.dayofweek
    # Spark no soporta TIMESTAMP(NANOS) en Parquet; pandas usa ns por defecto.
    df["timestamp"] = df["timestamp"].astype("datetime64[us, UTC]")
    return df


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--rows", type=int, default=600_000)
    parser.add_argument("--out", default="../../data/processed/incidents")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    df = generate(args.rows, args.seed)

    os.makedirs(args.out, exist_ok=True)
    # Particionado por zona en el propio Parquet -- decision independiente de
    # como fragmenta CockroachDB la tabla tickets (por fecha_apertura, ver
    # ADR-0003): este dataset de telemetria es una tabla distinta, y zone sigue
    # siendo la columna de menor cardinalidad util para el layout fisico del
    # Parquet (client_id tiene demasiados valores distintos para particionar por el).
    df.to_parquet(args.out, engine="pyarrow", partition_cols=["zone"], index=False)

    print(f"Dataset generado: {len(df):,} filas -> {args.out}")
    print(df.head())
    print("\nDistribución por zona:")
    print(df["zone"].value_counts())
    print("\nDistribución por tipo de incidencia:")
    print(df["incident_type"].value_counts())


if __name__ == "__main__":
    main()
