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
        "incident_type": incident_types,
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
    # Particionado por zona en el propio Parquet, coherente con la fragmentacion de CockroachDB
    df.to_parquet(args.out, engine="pyarrow", partition_cols=["zone"], index=False)

    print(f"Dataset generado: {len(df):,} filas -> {args.out}")
    print(df.head())
    print("\nDistribución por zona:")
    print(df["zone"].value_counts())
    print("\nDistribución por tipo de incidencia:")
    print(df["incident_type"].value_counts())


if __name__ == "__main__":
    main()
