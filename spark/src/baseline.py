"""
baseline.py — Baseline secuencial en pandas (Paso 6, Modulo E de la Guia de
Entrega 3, D4.2 de la rubrica: "Ambos scripts, mismos datos, resultados
numericos comparables").

Reproduce las mismas 5 transformaciones de transformations.py (Spark) usando
pandas + scikit-learn puro, sobre el MISMO dataset Parquet, para poder comparar
tiempos y validar que Spark realmente aporta un speedup y no solo overhead de
coordinacion distribuida sobre un dataset que cabria en memoria de un solo nodo.

Uso:
    python baseline.py --data ../../data/processed/incidents --out-json ../results/baseline.json

Requisitos:
    pip install pandas pyarrow scikit-learn --break-system-packages
"""

import argparse
import json
import time

import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import LabelEncoder

ZONES = ["QUEVEDO_CENTRO", "QUEVEDO_NORTE", "QUEVEDO_SUR"]
N_CLIENTS_PER_ZONE = 30_000  # debe coincidir con generate_dataset.N_CLIENTS_PER_ZONE

SPANISH_STOPWORDS = frozenset({
    "el", "la", "los", "las", "de", "del", "en", "y", "a", "un", "una", "no",
    "mi", "es", "que", "con", "se", "muy", "muchisimo", "todas", "esta",
})


def build_clients_dim() -> pd.DataFrame:
    """Misma dimension sintetica de clientes que transformations.build_clients_dim
    en el lado Spark, con el mismo esquema de id que genera generate_dataset.py."""
    rows = [
        (f"{zone}-CLIENTE-{i:05d}", zone, f"Cliente {i:05d}")
        for zone in ZONES
        for i in range(1, N_CLIENTS_PER_ZONE + 1)
    ]
    return pd.DataFrame(rows, columns=["client_id", "client_zone", "client_name"])


def t1_filter_valid_resolved(df: pd.DataFrame) -> pd.DataFrame:
    """T1 (filtrado) — equivalente pandas de transformations.t1_filter_valid_resolved."""
    return df[df["resolved"] & df["client_id"].notna() & df["description"].notna()].copy()


def t2_join_clients(df: pd.DataFrame, clients_dim: pd.DataFrame) -> pd.DataFrame:
    """T2 (join) — equivalente pandas de transformations.t2_join_clients."""
    return df.merge(clients_dim, on="client_id", how="left")


def t3_recurrence_window(df: pd.DataFrame) -> pd.DataFrame:
    """T3 (ventana) — pandas no tiene funciones de ventana nativas de SQL, pero
    groupby().cumcount() + groupby().shift() son el equivalente directo de
    row_number()/lag() sobre Window.partitionBy("client_id").orderBy("timestamp")."""
    df = df.sort_values(["client_id", "timestamp"]).copy()
    grouped = df.groupby("client_id")["timestamp"]
    df["incident_seq"] = grouped.cumcount() + 1
    df["previous_timestamp"] = grouped.shift(1)
    return df


def t4_recurrence_flag(df: pd.DataFrame) -> pd.DataFrame:
    """T4 (tipos temporales) — equivalente pandas de transformations.t4_recurrence_flag."""
    df = df.copy()
    df["incident_date"] = df["timestamp"].dt.date
    df["days_since_previous"] = (
        df["timestamp"] - df["previous_timestamp"]
    ).dt.total_seconds() / 86400.0
    df["is_recurring_30d"] = (df["incident_seq"] > 1) & (df["days_since_previous"] <= 30)
    return df


def t5_cluster_by_text_and_zone(df: pd.DataFrame, k: int = 5, seed: int = 42) -> pd.DataFrame:
    """T5 (ML) — equivalente pandas/scikit-learn de transformations.t5_cluster_by_text_and_zone:
    TF-IDF sobre la descripcion + zona codificada (LabelEncoder, equivalente a
    StringIndexer de Spark ML), agrupadas con KMeans."""
    df = df.copy()
    tfidf = TfidfVectorizer(stop_words=list(SPANISH_STOPWORDS), max_features=256)
    text_features = tfidf.fit_transform(df["description"]).toarray()

    zone_index = LabelEncoder().fit_transform(df["zone"]).reshape(-1, 1)
    features = np.hstack([text_features, zone_index])

    kmeans = KMeans(n_clusters=k, random_state=seed, n_init=10)
    df["cluster"] = kmeans.fit_predict(features)
    return df[["incident_id", "client_id", "zone", "incident_type", "description", "cluster"]]


def run_pipeline(data_path: str) -> dict:
    timings = {}

    t0 = time.perf_counter()
    df = pd.read_parquet(data_path, engine="pyarrow")
    timings["load"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r1 = t1_filter_valid_resolved(df)
    timings["t1_filter_valid_resolved"] = time.perf_counter() - t0

    clients_dim = build_clients_dim()

    t0 = time.perf_counter()
    r2 = t2_join_clients(r1, clients_dim)
    timings["t2_join_clients"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r3 = t3_recurrence_window(r2)
    timings["t3_recurrence_window"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r4 = t4_recurrence_flag(r3)
    timings["t4_recurrence_flag"] = time.perf_counter() - t0

    t0 = time.perf_counter()
    r5 = t5_cluster_by_text_and_zone(r1)
    timings["t5_ml_clustering"] = time.perf_counter() - t0

    timings["total"] = sum(timings.values())
    return timings, r4, r5


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="../../data/processed/incidents")
    parser.add_argument("--out-json", default=None)
    args = parser.parse_args()

    timings, reincidencia_df, clustering_df = run_pipeline(args.data)

    print("\n=== Tiempos (baseline pandas, secuencial, sin Spark) ===")
    for k, v in timings.items():
        print(f"  {k}: {v:.2f}s")

    print(f"\nFilas reincidentes en 30 dias: {int(reincidencia_df['is_recurring_30d'].sum()):,}")
    print("\nDistribucion de incidencias por cluster de texto+zona (T5):")
    print(clustering_df["cluster"].value_counts().sort_index())

    if args.out_json:
        with open(args.out_json, "w") as f:
            json.dump({"master": "pandas-sequential", "timings": timings}, f, indent=2)


if __name__ == "__main__":
    main()
