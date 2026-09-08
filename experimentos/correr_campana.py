# -*- coding: utf-8 -*-
"""
correr_campana.py -- Campana de medicion real del Modulo G ampliado (Adiciones 1-3,
docs/adr/0008-correl-incidencias.md), Entrega 4, equipo ACC.

Ejecuta los escenarios de averia de la Seccion 5.4 de la Guia de reutilizacion
(Esc-2, Esc-3, Esc-4) bajo las tres estrategias CORREL (c0, c1, c2), con N repeticiones,
usando el inyector y el analizador ya existentes (experimentos/inyector_averias.py,
experimentos/analizar_correlacion.py) contra el stack real levantado con
`docker compose up -d --build`.

ESCALA: reducida frente al protocolo completo (10 repeticiones, 100/500 abonados por
escenario) por restriccion de tiempo -- ver docs/experimentos/protocolo-e4.md seccion 3.
Se declara explicitamente el numero real de repeticiones y abonados usados, siguiendo el
mismo criterio de honestidad que docs/experimentos/evaluacion_iso25010.md.

Entre cada repeticion se trunca `incidencias`/`incidencia_tickets` (NO `tickets`, que
conserva los datos de demo) para eliminar cualquier contaminacion entre corridas -- el
CorrelationService busca candidatas por zona+ventana de 15 min sin filtrar por correlMode
(ver services/svc-principal .../CorrelationService.java), asi que sin este reinicio una
corrida podria fusionarse con incidencias de una corrida anterior.

Uso (con el stack levantado):
    python experimentos/correr_campana.py --reps 5
"""
import argparse
import csv
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

import numpy as np
from scipy import stats

sys.path.insert(0, str(Path(__file__).parent))
import inyector_averias as inyector  # noqa: E402

GATEWAY = "http://localhost:8000"
CLIENTE_EMAIL = "cliente@test.com"
CLIENTE_PASSWORD = "Passw0rd!"
RESULTS_DIR = Path(__file__).parent / "resultados"
VERDAD_CAMPO_CSV = RESULTS_DIR / "verdad_campo.csv"
CORRIDAS_CSV = RESULTS_DIR / "correlacion_corridas.csv"
ANALISIS_JSON = RESULTS_DIR / "correlacion_analisis.json"

# Escenarios de la Seccion 5.4 de la guia, a escala reducida (declarado explicitamente,
# ver docs/experimentos/protocolo-e4.md):
ESCENARIOS = {
    "Esc-2_averia_masiva": {"zonas": ["QUEVEDO_CENTRO"], "abonados": 20, "severidad": "ALTA"},
    "Esc-3_averia_mayor": {"zonas": ["QUEVEDO_CENTRO"], "abonados": 60, "severidad": "ALTA"},
    "Esc-4_dos_averias_simultaneas": {
        "zonas": ["QUEVEDO_CENTRO", "QUEVEDO_NORTE"], "abonados": 20, "severidad": "ALTA",
    },
}
MODOS = ["c0", "c1", "c2"]


def truncar_incidencias():
    """Limpia el estado de correlacion entre corridas (no toca `tickets`)."""
    subprocess.run(
        ["docker", "exec", "roach1", "cockroach", "sql", "--insecure",
         "--database=ticket_db",
         "--execute=TRUNCATE incidencia_tickets, incidencias CASCADE;"],
        check=True, capture_output=True, text=True,
    )


def cambiar_modo(modo: str):
    """Recrea ticket-service con CORREL=<modo> (se lee solo al arrancar el bean)."""
    import os
    env = dict(os.environ)
    env["CORREL"] = modo
    subprocess.run(
        ["docker", "compose", "up", "-d", "--no-deps", "--force-recreate", "ticket-service"],
        cwd=Path(__file__).parent.parent, env=env, check=True, capture_output=True, text=True,
    )
    # Espera a que el healthcheck vuelva a reportar sano tras el reinicio.
    for _ in range(60):
        r = subprocess.run(
            ["docker", "inspect", "-f", "{{.State.Health.Status}}", "ticket-service"],
            capture_output=True, text=True,
        )
        if r.stdout.strip() == "healthy":
            return
        time.sleep(2)
    raise RuntimeError("ticket-service no volvio a estar healthy tras cambiar CORREL")


def correr_escenario(nombre: str, cfg: dict, modo: str, rep: int) -> dict:
    prefijo = f"{nombre}-{modo}-r{rep}"
    token = inyector.login()
    verdad_local = set()
    for zona in cfg["zonas"]:
        for i in range(cfg["abonados"]):
            abonado_id = f"{prefijo}-{zona}-{i}"
            ticket_id = inyector.crear_ticket(token, zona, abonado_id)
            inyector.enviar_telemetria_equipo(zona, abonado_id, cfg["severidad"])
            verdad_local.add((zona, ticket_id))

    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    escribir_encabezado = not VERDAD_CAMPO_CSV.exists()
    with open(VERDAD_CAMPO_CSV, "a", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        if escribir_encabezado:
            w.writerow(["escenario", "modo", "repeticion", "zone", "ticket_id"])
        for zona, tid in verdad_local:
            w.writerow([nombre, modo, rep, zona, tid])

    time.sleep(1.5)  # margen para que la correlacion (y c2 via gRPC) termine de procesar

    resultado_por_zona = []
    for zona in cfg["zonas"]:
        req = urllib.request.Request(
            f"{GATEWAY}/api/v1/tickets/incidencias?zone={zona}",
            headers={"Authorization": f"Bearer {token}"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            incidencias = json.load(resp)["data"]
        verdad_zona = {tid for z, tid in verdad_local if z == zona}
        mejor = max(incidencias, key=lambda inc: len(set(inc["ticketIds"]) & verdad_zona), default=None)
        incidencias_efectivas = len(incidencias)
        if mejor is None or not (set(mejor["ticketIds"]) & verdad_zona):
            precision = exhaustividad = 0.0
            interseccion = 0
            tam_incidencia = 0
        else:
            conjunto = set(mejor["ticketIds"])
            interseccion = len(conjunto & verdad_zona)
            precision = interseccion / len(conjunto)
            exhaustividad = interseccion / len(verdad_zona)
            tam_incidencia = len(conjunto)
        resultado_por_zona.append({
            "escenario": nombre, "modo": modo, "repeticion": rep, "zone": zona,
            "incidencias_efectivas": incidencias_efectivas,
            "tickets_averia": len(verdad_zona),
            "tickets_en_incidencia_elegida": tam_incidencia,
            "interseccion": interseccion,
            "precision": round(precision, 4),
            "exhaustividad": round(exhaustividad, 4),
        })
    return resultado_por_zona


def vargha_delaney_a(x, y):
    """A de Vargha-Delaney: P(muestra de x > muestra de y), con empates a 0.5."""
    n1, n2 = len(x), len(y)
    if n1 == 0 or n2 == 0:
        return None
    u, _ = stats.mannwhitneyu(x, y, alternative="greater")
    return float(u) / (n1 * n2)


def binomial_ci95(k: int, n: int):
    if n == 0:
        return (None, None)
    p = k / n
    se = (p * (1 - p) / n) ** 0.5
    return (max(0.0, p - 1.96 * se), min(1.0, p + 1.96 * se))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reps", type=int, default=5)
    args = parser.parse_args()

    if VERDAD_CAMPO_CSV.exists():
        VERDAD_CAMPO_CSV.unlink()
    filas = []
    inicio_campana = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())

    for modo in MODOS:
        print(f"\n=== Cambiando a CORREL={modo} ===")
        cambiar_modo(modo)
        for nombre, cfg in ESCENARIOS.items():
            for rep in range(1, args.reps + 1):
                truncar_incidencias()
                print(f"  {nombre} / {modo} / rep {rep}/{args.reps} ...", end=" ", flush=True)
                resultados = correr_escenario(nombre, cfg, modo, rep)
                filas.extend(resultados)
                print("ok:", [f"{r['zone']}: P={r['precision']:.2f} R={r['exhaustividad']:.2f}" for r in resultados])

    with open(CORRIDAS_CSV, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0].keys()))
        w.writeheader()
        w.writerows(filas)
    print(f"\nCorridas guardadas en {CORRIDAS_CSV} ({len(filas)} filas)")

    # ---- Analisis agregado por modo ----
    analisis = {"inicio_utc": inicio_campana, "repeticiones_por_condicion": args.reps,
                "escenarios": {k: {"abonados": v["abonados"], "zonas": v["zonas"]} for k, v in ESCENARIOS.items()},
                "por_modo": {}}
    for modo in MODOS:
        precisiones = [r["precision"] for r in filas if r["modo"] == modo]
        exhaustividades = [r["exhaustividad"] for r in filas if r["modo"] == modo]
        incidencias_efectivas = [r["incidencias_efectivas"] for r in filas if r["modo"] == modo]
        analisis["por_modo"][modo] = {
            "n": len(precisiones),
            "precision_media": round(float(np.mean(precisiones)), 4),
            "precision_mediana": round(float(np.median(precisiones)), 4),
            "exhaustividad_media": round(float(np.mean(exhaustividades)), 4),
            "exhaustividad_mediana": round(float(np.median(exhaustividades)), 4),
            "incidencias_efectivas_media": round(float(np.mean(incidencias_efectivas)), 2),
        }

    # Contraste c0 vs c2 (linea base sin correlacion vs. la mas informada) en exhaustividad,
    # que es la metrica donde se espera la mayor diferencia (c0 nunca agrupa nada).
    c0 = [r["exhaustividad"] for r in filas if r["modo"] == "c0"]
    c2 = [r["exhaustividad"] for r in filas if r["modo"] == "c2"]
    if len(set(c0)) > 1 or len(set(c2)) > 1:
        stat, p = stats.mannwhitneyu(c2, c0, alternative="two-sided")
    else:
        stat, p = float("nan"), 1.0 if c0 == c2 else 0.0
    a12 = vargha_delaney_a(c2, c0)
    analisis["contraste_c2_vs_c0_exhaustividad"] = {
        "prueba": "Mann-Whitney U (bilateral)",
        "estadistico_u": float(stat) if stat == stat else None,
        "p_valor": float(p),
        "vargha_delaney_a_c2_mayor_c0": a12,
    }

    # Fusion erronea en Esc-4 (dos averias en zonas distintas): un ticket de la zona A
    # que termino en una incidencia junto con tickets de la zona B es estructuralmente
    # imposible aqui porque la correlacion filtra por zona (findByZoneAndCreatedAtAfter) --
    # se reporta explicitamente en vez de omitirlo.
    esc4_rows = [r for r in filas if r["escenario"] == "Esc-4_dos_averias_simultaneas"]
    fusiones = sum(1 for r in esc4_rows if r["tickets_en_incidencia_elegida"] > r["tickets_averia"] and r["interseccion"] < r["tickets_en_incidencia_elegida"])
    lo, hi = binomial_ci95(fusiones, len(esc4_rows))
    analisis["fusion_erronea_esc4"] = {
        "n_corridas": len(esc4_rows), "fusiones_observadas": fusiones,
        "ic95_binomial": [lo, hi],
        "nota": "La fusion cruzada de zona es estructuralmente imposible en esta implementacion "
                "(la correlacion filtra por zona antes de agrupar); 0 observadas es el resultado "
                "esperado y valida el particionado por zona, no una ausencia de prueba.",
    }

    with open(ANALISIS_JSON, "w", encoding="utf-8") as f:
        json.dump(analisis, f, indent=2, ensure_ascii=False)
    print(f"Analisis guardado en {ANALISIS_JSON}")
    print(json.dumps(analisis, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
