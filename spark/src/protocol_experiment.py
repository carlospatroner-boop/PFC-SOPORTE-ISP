"""
protocol_experiment.py — Protocolo experimental JCR (Paso 7 / Modulo F de la Guia
de Entrega 3, D4.3 de la rubrica).

Hipotesis operativa:
  H1: el pipeline paralelo con N cores presenta menor tiempo medio de ejecucion
      que el baseline secuencial (pandas) para |D| >= 5*10^5 registros.
  H0: no hay diferencia.

Diseno: bloque completo con r=10 repeticiones por nivel de N en {1,2,4,8} y por
el baseline secuencial; se descartan la primera y la ultima repeticion
(calentamiento de JVM/cache de SO y enfriamiento), quedando 8 muestras validas
por nivel.

Analisis: media, desviacion tipica e intervalo de confianza al 95%
(t_bar +/- 1.96*s/sqrt(r)) por nivel; contraste de H1 comparando el mejor nivel
de paralelismo (N=8) contra el baseline secuencial, emparejado por indice de
repeticion -- prueba t pareada si Shapiro-Wilk no rechaza normalidad de las
diferencias (p > 0.05), Wilcoxon signed-rank en caso contrario.

Salidas (en --results-dir):
  raw.csv            -- una fila por repeticion (modo, N, repeticion, tiempo_total_s)
  summary_stats.csv   -- media/desviacion/IC95% por nivel
  stats_test.json     -- prueba usada, estadistico, p-valor, conclusion
  boxplot.png         -- diagrama de caja del tiempo total por nivel (300 dpi)

Uso:
    python protocol_experiment.py --data ../data/processed/incidents --reps 10
"""

import argparse
import csv
import json
import subprocess
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from scipy import stats

N_VALUES = [1, 2, 4, 8]
WARMUP_COOLDOWN = 1  # se descarta 1 repeticion al inicio y 1 al final (r=10 -> 8 validas)


def run_spark_repetition(data_path: str, n: int, tmp_json: str) -> float:
    subprocess.run(
        [sys.executable, "pipeline.py", "--data", data_path, "--master", f"local[{n}]", "--out-json", tmp_json],
        check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    with open(tmp_json) as f:
        return json.load(f)["timings"]["total"]


def run_baseline_repetition(data_path: str, tmp_json: str) -> float:
    subprocess.run(
        [sys.executable, "baseline.py", "--data", data_path, "--out-json", tmp_json],
        check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    with open(tmp_json) as f:
        return json.load(f)["timings"]["total"]


def collect_samples(data_path: str, reps: int, results_dir: Path) -> dict:
    """Corre r repeticiones para cada N y para el baseline secuencial. Devuelve
    {mode: [tiempos_totales]} con las r muestras crudas (sin descartar todavia)."""
    tmp_json = str(results_dir / "_tmp_timing.json")
    samples = {}

    print(f"\n>>> Baseline secuencial (pandas): {reps} repeticiones")
    baseline_times = []
    for rep in range(1, reps + 1):
        t = run_baseline_repetition(data_path, tmp_json)
        print(f"    rep {rep}/{reps}: {t:.2f}s")
        baseline_times.append(t)
    samples["baseline"] = baseline_times

    for n in N_VALUES:
        print(f"\n>>> Spark local[{n}]: {reps} repeticiones")
        times = []
        for rep in range(1, reps + 1):
            t = run_spark_repetition(data_path, n, tmp_json)
            print(f"    rep {rep}/{reps}: {t:.2f}s")
            times.append(t)
        samples[f"N{n}"] = times

    Path(tmp_json).unlink(missing_ok=True)
    return samples


def trimmed(values: list) -> list:
    """Descarta la primera y ultima repeticion (calentamiento/enfriamiento)."""
    if len(values) <= 2 * WARMUP_COOLDOWN:
        return values
    return values[WARMUP_COOLDOWN:-WARMUP_COOLDOWN]


def confidence_interval_95(values: list) -> tuple:
    arr = np.array(values)
    mean = arr.mean()
    std = arr.std(ddof=1)
    half_width = 1.96 * std / np.sqrt(len(arr))
    return mean, std, mean - half_width, mean + half_width


def analyze(raw_samples: dict, results_dir: Path) -> dict:
    trimmed_samples = {mode: trimmed(values) for mode, values in raw_samples.items()}

    # raw.csv -- todas las repeticiones, incluidas las descartadas (transparencia total)
    with open(results_dir / "raw.csv", "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["modo", "repeticion", "tiempo_total_s", "descartada_calentamiento_enfriamiento"])
        for mode, values in raw_samples.items():
            kept_indices = set(range(WARMUP_COOLDOWN, len(values) - WARMUP_COOLDOWN)) if len(values) > 2 * WARMUP_COOLDOWN else set(range(len(values)))
            for i, v in enumerate(values):
                writer.writerow([mode, i + 1, round(v, 4), "no" if i in kept_indices else "si"])

    # summary_stats.csv -- media/desviacion/IC95% con las muestras ya recortadas
    summary = {}
    with open(results_dir / "summary_stats.csv", "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["modo", "n_muestras_validas", "media_s", "desviacion_s", "ic95_inferior_s", "ic95_superior_s"])
        for mode, values in trimmed_samples.items():
            mean, std, lo, hi = confidence_interval_95(values)
            summary[mode] = {"mean": mean, "std": std, "ci_low": lo, "ci_high": hi, "n": len(values)}
            writer.writerow([mode, len(values), round(mean, 3), round(std, 3), round(lo, 3), round(hi, 3)])

    # Contraste de H1: mejor nivel de paralelismo (N=8) vs baseline secuencial,
    # emparejado por indice de repeticion.
    parallel = np.array(trimmed_samples[f"N{max(N_VALUES)}"])
    sequential = np.array(trimmed_samples["baseline"])
    n_pairs = min(len(parallel), len(sequential))
    parallel, sequential = parallel[:n_pairs], sequential[:n_pairs]
    differences = sequential - parallel

    shapiro_stat, shapiro_p = stats.shapiro(differences)
    normal = shapiro_p > 0.05

    if normal:
        test_name = "prueba t pareada (t-test relacionado)"
        stat, p_value = stats.ttest_rel(sequential, parallel)
    else:
        test_name = "Wilcoxon signed-rank"
        stat, p_value = stats.wilcoxon(sequential, parallel)

    reject_h0 = p_value < 0.05 and differences.mean() > 0
    stats_result = {
        "hipotesis": "H1: el pipeline paralelo (N=8) tiene menor tiempo medio que el baseline secuencial",
        "shapiro_wilk": {"estadistico": float(shapiro_stat), "p_valor": float(shapiro_p), "normal": normal},
        "prueba_usada": test_name,
        "estadistico": float(stat),
        "p_valor": float(p_value),
        "diferencia_media_s": float(differences.mean()),
        "conclusion": "se rechaza H0: el pipeline paralelo es significativamente mas rapido" if reject_h0
                      else "no se rechaza H0 (sin diferencia significativa al 95%)",
    }
    with open(results_dir / "stats_test.json", "w") as f:
        json.dump(stats_result, f, indent=2, ensure_ascii=False)

    # boxplot.png -- distribucion del tiempo total por modo (300 dpi, para el manuscrito)
    labels = ["baseline"] + [f"N={n}" for n in N_VALUES]
    data_for_plot = [trimmed_samples["baseline"]] + [trimmed_samples[f"N{n}"] for n in N_VALUES]
    plt.figure(figsize=(8, 5))
    plt.boxplot(data_for_plot, tick_labels=labels)
    plt.ylabel("Tiempo total (s)")
    plt.title("Distribucion del tiempo total por modo (r=10, recortado a 8 muestras validas)")
    plt.grid(True, alpha=0.3, axis="y")
    plt.tight_layout()
    plt.savefig(results_dir / "boxplot.png", dpi=300)
    plt.close()

    return {"summary": summary, "stats_test": stats_result}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="../data/processed/incidents")
    parser.add_argument("--reps", type=int, default=10)
    parser.add_argument("--results-dir", default="../results/protocol")
    args = parser.parse_args()

    results_dir = Path(args.results_dir)
    results_dir.mkdir(parents=True, exist_ok=True)

    raw_samples = collect_samples(args.data, args.reps, results_dir)
    with open(results_dir / "raw_samples_backup.json", "w") as f:
        json.dump(raw_samples, f, indent=2)

    result = analyze(raw_samples, results_dir)

    print("\n=== Resumen (media +/- IC95%, 8 muestras validas por nivel) ===")
    for mode, s in result["summary"].items():
        print(f"  {mode}: {s['mean']:.2f}s [{s['ci_low']:.2f}, {s['ci_high']:.2f}]")

    print(f"\n=== Contraste de H1 ===")
    print(json.dumps(result["stats_test"], indent=2, ensure_ascii=False))

    print(f"\nArchivos guardados en {results_dir}/: raw.csv, summary_stats.csv, stats_test.json, boxplot.png")


if __name__ == "__main__":
    main()
