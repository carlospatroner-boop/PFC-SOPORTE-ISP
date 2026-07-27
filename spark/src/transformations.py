"""
transformations.py — Las 5 transformaciones exigidas por la Guia de Entrega 3
(Modulo E, Seccion 5.6): filtrado, join entre tablas colocalizadas, agregacion
con ventanas, transformacion de tipos temporales, y una operacion de ML.

Aplicadas al objetivo especifico del equipo ACC (Tabla 1 de la guia, columna
"Analitica paralela"): analisis de reincidencia y agrupamiento (clustering) de
incidencias por texto y zona.

  T1 filtrado                    -> incidencias resueltas y con datos completos
  T2 join (shuffle)              -> colocalizacion incidencias JOIN clientes por client_id
  T3 ventana (shuffle)           -> reincidencia: orden y timestamp anterior por cliente
  T4 tipos temporales            -> dias desde la incidencia anterior + bandera de reincidencia
  T5 ML (StringIndexer + KMeans) -> clustering de incidencias por texto (TF-IDF) y zona

Cada funcion recibe y devuelve un DataFrame de Spark, para poder medir el tiempo
de cada una por separado desde pipeline.py / amdahl_analysis.py.
"""

from pyspark.ml import Pipeline
from pyspark.ml.clustering import KMeans
from pyspark.ml.feature import HashingTF, IDF, StopWordsRemover, StringIndexer, Tokenizer, VectorAssembler
from pyspark.sql import DataFrame, functions as F
from pyspark.sql.window import Window


def t1_filter_valid_resolved(df: DataFrame) -> DataFrame:
    """T1 (filtrado, sin shuffle) — solo incidencias resueltas y con los campos
    que el resto del pipeline necesita (client_id, description); base valida
    para el analisis de reincidencia y de clustering."""
    return df.filter(
        (F.col("resolved") == True)  # noqa: E712 (comparacion explicita, estilo Spark)
        & F.col("client_id").isNotNull()
        & F.col("description").isNotNull()
    )


def build_clients_dim(spark, zones: list, n_clients_per_zone: int) -> DataFrame:
    """Dimension de clientes para el join de T2. Mismo esquema de id que genera
    generate_dataset.py (`{zone}-CLIENTE-{n:05d}`) para que el join encuentre
    coincidencias reales, no una tabla vacia."""
    rows = [
        (f"{zone}-CLIENTE-{i:05d}", zone, f"Cliente {i:05d}")
        for zone in zones
        for i in range(1, n_clients_per_zone + 1)
    ]
    return spark.createDataFrame(rows, ["client_id", "client_zone", "client_name"])


def t2_join_clients(df: DataFrame, clients_dim: DataFrame) -> DataFrame:
    """T2 (shuffle: join) — colocalizacion incidencias JOIN clientes por
    client_id (Tabla 1 de la guia de E3, columna "Fragmentacion + colocalizacion"
    del equipo ACC: "colocalizacion clientes bowtie tickets por cliente_id")."""
    return df.join(clients_dim, on="client_id", how="left")


def t3_recurrence_window(df: DataFrame) -> DataFrame:
    """T3 (shuffle: funcion de ventana) — para cada cliente, ordena sus
    incidencias por fecha y calcula el numero de orden (incident_seq) y el
    timestamp de la incidencia anterior del MISMO cliente (previous_timestamp).
    Es la base del analisis de reincidencia: un cliente con incident_seq > 1 ya
    reporto antes."""
    w = Window.partitionBy("client_id").orderBy("timestamp")
    return (
        df.withColumn("incident_seq", F.row_number().over(w))
          .withColumn("previous_timestamp", F.lag("timestamp").over(w))
    )


def t4_recurrence_flag(df: DataFrame) -> DataFrame:
    """T4 (transformacion de tipos temporales, sin shuffle) — a partir del
    timestamp anterior calculado en T3, deriva la fecha (sin hora) de la
    incidencia y los dias transcurridos desde la incidencia previa del cliente;
    marca como "reincidente en 30 dias" a quien reporto de nuevo dentro de ese
    plazo -- la metrica de reincidencia que pide la Tabla 1 para el equipo ACC."""
    days_since_previous = (
        F.col("timestamp").cast("long") - F.col("previous_timestamp").cast("long")
    ) / F.lit(86400.0)
    return (
        df.withColumn("incident_date", F.to_date("timestamp"))
          .withColumn("days_since_previous", days_since_previous)
          .withColumn(
              "is_recurring_30d",
              (F.col("incident_seq") > 1) & (F.col("days_since_previous") <= 30)
          )
    )


def t5_cluster_by_text_and_zone(df: DataFrame, k: int = 5, seed: int = 42) -> DataFrame:
    """T5 (ML) — agrupamiento (clustering) de incidencias por texto y zona
    (Tabla 1 de la guia de E3, columna "Analitica paralela" del equipo ACC):
    vectoriza la descripcion libre con TF-IDF (Tokenizer + StopWordsRemover +
    HashingTF + IDF), codifica la zona con StringIndexer (satisface tambien el
    ejemplo explicito de la guia, "StringIndexer, Bucketizer o similar"), y
    agrupa con KMeans sobre la union de ambas features."""
    tokenizer = Tokenizer(inputCol="description", outputCol="words")
    remover = StopWordsRemover(
        inputCol="words", outputCol="filtered_words",
        stopWords=StopWordsRemover.loadDefaultStopWords("spanish"),
    )
    hashing_tf = HashingTF(inputCol="filtered_words", outputCol="tf", numFeatures=256)
    idf = IDF(inputCol="tf", outputCol="text_features")
    zone_indexer = StringIndexer(inputCol="zone", outputCol="zone_index")
    assembler = VectorAssembler(inputCols=["text_features", "zone_index"], outputCol="features")
    kmeans = KMeans(k=k, seed=seed, featuresCol="features", predictionCol="cluster")

    ml_pipeline = Pipeline(stages=[tokenizer, remover, hashing_tf, idf, zone_indexer, assembler, kmeans])
    model = ml_pipeline.fit(df)
    return model.transform(df).select(
        "incident_id", "client_id", "zone", "incident_type", "description", "cluster"
    )
