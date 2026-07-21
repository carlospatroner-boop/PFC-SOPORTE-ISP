"""
load_write.py — Inyector de carga para la prueba de tolerancia a fallos (Paso 4).

Inserta ~100 filas/segundo durante 3 minutos contra el cluster CockroachDB.
Mientras corre este script, en otra terminal se ejecuta:

    docker stop roach2

Se espera que las inserciones continuen sin perdida y sin errores en el log
(mientras 2 de los 3 nodos sigan vivos, Raft mantiene el quorum de mayoria).
El script registra, por cada insercion: timestamp, latencia (ms) y si hubo error.
Al finalizar escribe un CSV en results_fault_tolerance.csv con P50/P95 de latencia
por ventana de 10 segundos, para poder graficar el "antes / durante / despues" del
docker stop.

Requisitos:
    pip install psycopg2-binary

Uso:
    python load_write.py --host localhost --port 26257 --duration 180 --rate 100
"""

import argparse
import csv
import statistics
import time
import uuid
from datetime import datetime, timezone

import psycopg2

ZONES = ["QUEVEDO_CENTRO", "QUEVEDO_NORTE", "QUEVEDO_SUR"]
CATEGORIES = ["CONECTIVIDAD", "DNS", "HARDWARE", "CONFIGURACION", "VELOCIDAD"]
PRIORITIES = ["CRITICO", "ALTO", "MEDIO", "BAJO"]


def connect(host: str, port: int):
    return psycopg2.connect(
        host=host,
        port=port,
        dbname="ticket_db",
        user="root",
        sslmode="disable",
    )


def insert_one(conn, i: int) -> tuple[float, bool]:
    zone = ZONES[i % len(ZONES)]
    start = time.perf_counter()
    ok = True
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO tickets (zone, client_id, category, priority, status, description)
                VALUES (%s, %s, %s, %s, 'NUEVO', %s)
                """,
                (
                    zone,
                    str(uuid.uuid4()),
                    CATEGORIES[i % len(CATEGORIES)],
                    PRIORITIES[i % len(PRIORITIES)],
                    f"Ticket de prueba de carga #{i} — fault tolerance test",
                ),
            )
            conn.commit()
    except Exception as exc:  # noqa: BLE001 — se quiere registrar cualquier fallo, no ocultarlo
        conn.rollback()
        print(f"[{datetime.now(timezone.utc).isoformat()}] ERROR en insercion #{i}: {exc}")
        ok = False
    latency_ms = (time.perf_counter() - start) * 1000
    return latency_ms, ok


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", type=int, default=26257)
    parser.add_argument("--duration", type=int, default=180, help="segundos totales de carga")
    parser.add_argument("--rate", type=int, default=100, help="inserciones objetivo por segundo")
    parser.add_argument("--out", default="results_fault_tolerance.csv")
    args = parser.parse_args()

    conn = connect(args.host, args.port)
    print(f"Conectado a {args.host}:{args.port}. Iniciando carga de {args.rate} filas/seg "
          f"durante {args.duration}s. Detene un nodo con 'docker stop roach2' cuando quieras.")

    interval = 1.0 / args.rate
    t_end = time.time() + args.duration
    i = 0
    window_latencies: list[float] = []
    window_start = time.time()
    rows = []  # (window_start_iso, p50_ms, p95_ms, errors_in_window, count_in_window)
    errors_in_window = 0

    while time.time() < t_end:
        loop_start = time.time()
        latency_ms, ok = insert_one(conn, i)
        i += 1
        window_latencies.append(latency_ms)
        if not ok:
            errors_in_window += 1

        if time.time() - window_start >= 10:
            p50 = statistics.median(window_latencies) if window_latencies else 0
            p95 = (
                statistics.quantiles(window_latencies, n=100)[94]
                if len(window_latencies) >= 20
                else max(window_latencies, default=0)
            )
            ts = datetime.now(timezone.utc).isoformat()
            rows.append((ts, round(p50, 2), round(p95, 2), errors_in_window, len(window_latencies)))
            print(f"[{ts}] ventana 10s -> P50={p50:.1f}ms P95={p95:.1f}ms "
                  f"errores={errors_in_window} filas={len(window_latencies)}")
            window_latencies = []
            errors_in_window = 0
            window_start = time.time()

        elapsed = time.time() - loop_start
        sleep_time = interval - elapsed
        if sleep_time > 0:
            time.sleep(sleep_time)

    conn.close()

    with open(args.out, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["window_start_utc", "p50_ms", "p95_ms", "errors", "rows_in_window"])
        writer.writerows(rows)

    print(f"Total de filas insertadas: {i}. Resultados por ventana en {args.out}")


if __name__ == "__main__":
    main()
