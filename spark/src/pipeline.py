"""
pipeline.py — Pipeline de procesamiento paralelo con Apache Spark (Paso 6).

Ejecuta las 5 transformaciones de transformations.py (filtrado, join
colocalizado, ventana de reincidencia, tipos temporales, clustering ML) sobre
el dataset de incidencias generado por generate_dataset.py, midiendo el tiempo
de cada una y del pipeline completo. Pensado para ser invocado con distinto
spark.master desde amdahl_analysis.py (N = 1, 2, 4, 8).

Uso directo (una sola corrida, para validar que todo funciona):
    python pipeline.py --data ../../data/processed/incidents --master "local[4]"

Requisitos:
    pip install pyspark --break-system-packages
"""

import argparse
import json
import time

from pyspark.sql import SparkSession

from transformations import (
    build_clients_dim,
    t1_filter_valid_resolved,
    t2_join_clients,
    t3_recurrence_window,
    t4_recurrence_flag,
    t5_cluster_by_text_and_zone,
)

ZONES = ["QUEVEDO_CENTRO", "QUEVEDO_NORTE", "QUEVEDO_SUR"]
N_CLIENTS_PER_ZONE = 30_000  # debe coincidir con generate_dataset.N_CLIENTS_PER_ZONE


def run_pipeline(spark, data_path: str, out_dir: str = None):
    timings = {}

    t0 = time.perf_counter()
    df = spark.read.parquet(data_path)
    df = df.repartition(200)  # fuerza particionamiento uniforme para que N workers se noten
    df.cache()
    df.count()  # materializa la cache antes de medir
    timings["load"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r1 = t1_filter_valid_resolved(df)
    r1.cache()
    r1.count()
    timings["t1_filter_valid_resolved"] = time.perf_counter() - t0

    clients_dim = build_clients_dim(spark, ZONES, N_CLIENTS_PER_ZONE)

    t0 = time.perf_counter()
    r2 = t2_join_clients(r1, clients_dim)
    r2.cache()
    r2.count()
    timings["t2_join_clients"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r3 = t3_recurrence_window(r2)
    r3.cache()
    r3.count()
    timings["t3_recurrence_window"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r4 = t4_recurrence_flag(r3)
    r4.cache()
    r4.count()
    timings["t4_recurrence_flag"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r5 = t5_cluster_by_text_and_zone(r1)
    r5.cache()
    r5.count()
    timings["t5_ml_clustering"] = time.perf_counter() - t0

    timings["total"] = sum(timings.values())

    if out_dir:
        r4.write.mode("overwrite").parquet(f"{out_dir}/reincidencia")
        r5.write.mode("overwrite").parquet(f"{out_dir}/clustering")

    return timings, r4, r5


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="../../data/processed/incidents")
    parser.add_argument("--master", default="local[4]")
    parser.add_argument("--out", default=None,
                         help="si se pasa, escribe reincidencia/ y clustering/ en Parquet dentro de esta carpeta")
    parser.add_argument("--out-json", default=None,
                         help="si se pasa, escribe los tiempos medidos en formato JSON")
    args = parser.parse_args()

    spark = (
        SparkSession.builder
        .master(args.master)
        .appName("acc-soporte-tecnico-pipeline")
        .config("spark.sql.shuffle.partitions", "200")
        .getOrCreate()
    )
    spark.sparkContext.setLogLevel("WARN")

    timings, reincidencia_df, clustering_df = run_pipeline(spark, args.data, args.out)

    print(f"\n=== Tiempos (master={args.master}) ===")
    for k, v in timings.items():
        print(f"  {k}: {v:.2f}s")

    print("\n=== Clientes reincidentes en 30 dias (T3+T4, muestra) ===")
    reincidencia_df.filter(reincidencia_df.is_recurring_30d).select(
        "client_id", "zone", "incident_seq", "days_since_previous"
    ).show(20, truncate=False)

    print("\n=== Distribucion de incidencias por cluster de texto+zona (T5) ===")
    clustering_df.groupBy("cluster").count().orderBy("cluster").show()

    if args.out_json:
        with open(args.out_json, "w") as f:
            json.dump({"master": args.master, "timings": timings}, f, indent=2)

    spark.stop()


if __name__ == "__main__":
    main()
