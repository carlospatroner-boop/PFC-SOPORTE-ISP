"""
pipeline.py — Pipeline de procesamiento paralelo con Apache Spark (Paso 6).

Ejecuta las 5 transformaciones de transformations.py sobre el dataset de
incidencias generado por generate_dataset.py, midiendo el tiempo de cada una
y del pipeline completo. Pensado para ser invocado con distinto spark.master
desde amdahl_analysis.py (N = 1, 2, 4, 8).

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
    build_technicians_dim,
    t1_incidents_by_zone_type,
    t2_join_technician_workload,
    t3_top_longest_incidents,
    t4_mttr_by_zone_hour,
    t5_problematic_zones,
)


def run_pipeline(spark, data_path: str) -> dict:
    timings = {}

    t0 = time.perf_counter()
    df = spark.read.parquet(data_path)
    df = df.repartition(200)  # fuerza particionamiento uniforme para que N workers se noten
    df.cache()
    df.count()  # materializa la cache antes de medir
    timings["load"] = time.perf_counter() - t0

    technicians = build_technicians_dim(spark)

    t0 = time.perf_counter()
    r1 = t1_incidents_by_zone_type(df)
    r1.count()
    timings["t1_groupby_zone_type"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r2 = t2_join_technician_workload(df, technicians)
    r2.count()
    timings["t2_join_technician"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r3 = t3_top_longest_incidents(df)
    r3.count()
    timings["t3_orderby_top_n"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r4 = t4_mttr_by_zone_hour(df)
    r4.cache()
    r4.count()
    timings["t4_mttr_groupby"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r5 = t5_problematic_zones(r4)
    r5.count()
    timings["t5_filter_no_shuffle"] = time.perf_counter() - t0

    timings["total"] = sum(timings.values())
    return timings, r5


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="../../data/processed/incidents")
    parser.add_argument("--master", default="local[4]")
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

    timings, result_df = run_pipeline(spark, args.data)

    print(f"\n=== Tiempos (master={args.master}) ===")
    for k, v in timings.items():
        print(f"  {k}: {v:.2f}s")

    print("\n=== Zonas problematicas detectadas (T5) ===")
    result_df.show(20, truncate=False)

    if args.out_json:
        with open(args.out_json, "w") as f:
            json.dump({"master": args.master, "timings": timings}, f, indent=2)

    spark.stop()


if __name__ == "__main__":
    main()
