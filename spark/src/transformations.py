"""
transformations.py — Las 5 transformaciones representativas del dominio (Paso 6).

Al menos 3 de las 5 producen shuffle entre particiones:
  T1 groupBy         -> shuffle
  T2 join            -> shuffle
  T3 orderBy         -> shuffle
  T4 window/groupBy  -> shuffle
  T5 filter/select   -> sin shuffle (deteccion de zonas problematicas sobre T4)

Cada funcion recibe y devuelve un DataFrame de Spark, para poder medir el tiempo
de cada una por separado desde pipeline.py / amdahl_analysis.py.
"""

from pyspark.sql import DataFrame, functions as F
from pyspark.sql.window import Window


def t1_incidents_by_zone_type(df: DataFrame) -> DataFrame:
    """T1 (shuffle: groupBy) — volumen de incidencias por zona y tipo."""
    return (
        df.groupBy("zone", "incident_type")
        .agg(
            F.count("*").alias("incident_count"),
            F.avg("duration_minutes").alias("avg_duration_min"),
        )
        .orderBy(F.desc("incident_count"))
    )


def t2_join_technician_workload(df: DataFrame, technicians: DataFrame) -> DataFrame:
    """T2 (shuffle: join) — cruce con dimension de tecnicos para calcular carga de trabajo."""
    technicians_dim = technicians.withColumnRenamed("zone", "technician_zone")
    return (
        df.join(technicians_dim, on="technician_id", how="left")
        .groupBy("technician_id", "full_name", "technician_zone")
        .agg(F.count("*").alias("tickets_atendidos"))
    )


def t3_top_longest_incidents(df: DataFrame, top_n: int = 100) -> DataFrame:
    """T3 (shuffle: orderBy global) — top-N incidencias mas largas, candidatas a revision manual."""
    return df.orderBy(F.desc("duration_minutes")).limit(top_n)


def t4_mttr_by_zone_hour(df: DataFrame) -> DataFrame:
    """T4 (shuffle: groupBy) — MTTR (Mean Time To Repair) por zona y hora del dia:
    permite detectar patrones temporales de fallas masivas (Tabla 1, fila ACC)."""
    return (
        df.filter(F.col("resolved") == True)  # noqa: E712 (comparacion explicita, estilo Spark)
        .groupBy("zone", "hour_of_day")
        .agg(
            F.avg("duration_minutes").alias("mttr_min"),
            F.count("*").alias("resolved_count"),
            F.sum(F.col("sla_breached").cast("int")).alias("sla_breaches"),
        )
    )


def t5_problematic_zones(mttr_df: DataFrame, mttr_threshold_min: float = 90.0) -> DataFrame:
    """T5 (sin shuffle: filter/select sobre un DataFrame ya agregado) —
    zonas/horas que superan el umbral de MTTR, es decir, "zonas problematicas"."""
    return (
        mttr_df.filter(F.col("mttr_min") > mttr_threshold_min)
        .select("zone", "hour_of_day", "mttr_min", "resolved_count", "sla_breaches")
        .orderBy(F.desc("mttr_min"))
    )


def build_technicians_dim(spark, n_technicians: int = 18):
    """Dimension pequeña de tecnicos para el join de T2 (se podria leer de CockroachDB
    via JDBC en la version final; aqui se genera en memoria para simplicidad del pipeline)."""
    rows = [(i, f"Tecnico_{i:02d}", ["QUEVEDO_CENTRO", "QUEVEDO_NORTE", "QUEVEDO_SUR"][i % 3])
            for i in range(1, n_technicians + 1)]
    return spark.createDataFrame(rows, ["technician_id", "full_name", "zone"])
