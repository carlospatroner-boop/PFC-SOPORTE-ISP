"""
amdahl_analysis.py — Medicion de speedup y contraste con la Ley de Amdahl (Paso 7).

Corre pipeline.py con spark.master=local[N] para N en {1,2,4,8}, mide el tiempo
total de cada corrida, calcula el speedup observado S(N) = T(1)/T(N), ajusta la
fraccion paralelizable p de la Ley de Amdahl:

        S(N) = 1 / ( (1-p) + p/N )

y genera dos figuras (300 dpi):
  1. fig_speedup.png            -> speedup observado vs. teorico (Amdahl) vs. N
  2. fig_time_per_transform.png -> tiempo de cada transformacion por N

Tambien reporta el speedup teorico maximo S(inf) = 1/(1-p) y, siguiendo a
Gustafson [9], una nota sobre como el speedup mejoraria si el tamaño del
problema escalara junto con N (en vez de mantenerse fijo, que es el supuesto
de Amdahl).

Uso:
    python amdahl_analysis.py --data ../../data/processed/incidents
"""

import argparse
import csv
import json
import subprocess
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from scipy.optimize import curve_fit

N_VALUES = [1, 2, 4, 8]


def amdahl_speedup(n, p):
    return 1.0 / ((1 - p) + p / n)


def run_all(data_path: str, results_dir: str):
    all_timings = []
    for n in N_VALUES:
        out_json = f"{results_dir}/timings_N{n}.json"
        print(f"\n>>> Ejecutando pipeline con local[{n}] ...")
        subprocess.run(
            [
                sys.executable, "pipeline.py",
                "--data", data_path,
                "--master", f"local[{n}]",
                "--out-json", out_json,
            ],
            check=True,
        )
        with open(out_json) as f:
            all_timings.append(json.load(f))
    return all_timings


def analyze(all_timings: list[dict], results_dir: str):
    ns = np.array(N_VALUES, dtype=float)
    total_times = np.array([t["timings"]["total"] for t in all_timings])
    t1 = total_times[0]
    speedup_observed = t1 / total_times

    # Ajuste de la fraccion paralelizable p por minimos cuadrados no lineales
    popt, _ = curve_fit(amdahl_speedup, ns, speedup_observed, p0=[0.8], bounds=(0, 1))
    p_fit = popt[0]
    s_inf = 1.0 / (1 - p_fit)

    # Speedup teorico de Amdahl con la p ajustada, evaluado en los mismos N
    speedup_theoretical = amdahl_speedup(ns, p_fit)

    # Workers necesarios para alcanzar el 90% del speedup teorico maximo (pregunta obligatoria Anexo B, Q3)
    target = 0.9 * s_inf
    n_range = np.linspace(1, 256, 5000)
    s_range = amdahl_speedup(n_range, p_fit)
    n_for_90pct = n_range[np.searchsorted(s_range, target)] if np.any(s_range >= target) else None

    print("\n=== Resultados Amdahl ===")
    print(f"Fraccion paralelizable ajustada p = {p_fit:.4f}")
    print(f"Speedup teorico maximo S(inf) = {s_inf:.2f}")
    if n_for_90pct:
        print(f"Workers necesarios para el 90% de S(inf): N ~= {n_for_90pct:.1f}")
    print("\nN\tT(N)s\tSpeedup_obs\tSpeedup_Amdahl_teorico")
    for n, t, so, st in zip(ns, total_times, speedup_observed, speedup_theoretical):
        print(f"{int(n)}\t{t:.2f}\t{so:.2f}\t{st:.2f}")

    # CSV de resultados (para la tabla del documento LaTeX)
    with open(f"{results_dir}/speedup_table.csv", "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["N", "tiempo_total_s", "speedup_observado", "speedup_amdahl_teorico"])
        for n, t, so, st in zip(ns, total_times, speedup_observed, speedup_theoretical):
            writer.writerow([int(n), round(t, 3), round(so, 3), round(st, 3)])

    with open(f"{results_dir}/amdahl_fit.json", "w") as f:
        json.dump({"p_fit": p_fit, "s_inf": s_inf,
                    "n_for_90pct_of_s_inf": float(n_for_90pct) if n_for_90pct else None}, f, indent=2)

    # Figura 1: speedup observado vs teorico
    plt.figure(figsize=(7, 5))
    plt.plot(ns, speedup_observed, "o-", label="Speedup observado")
    n_smooth = np.linspace(1, 8, 200)
    plt.plot(n_smooth, amdahl_speedup(n_smooth, p_fit), "--",
              label=f"Amdahl teorico (p={p_fit:.3f})")
    plt.axhline(s_inf, color="gray", linestyle=":", label=f"S(inf)={s_inf:.2f}")
    plt.xlabel("Numero de particiones/workers (N)")
    plt.ylabel("Speedup S(N)")
    plt.title("Speedup del pipeline vs. numero de particiones")
    plt.legend()
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(f"{results_dir}/fig_speedup.png", dpi=300)
    plt.close()

    # Figura 2: tiempo por transformacion, para cada N
    transform_keys = [k for k in all_timings[0]["timings"].keys() if k != "total"]
    plt.figure(figsize=(8, 5))
    width = 0.2
    x = np.arange(len(transform_keys))
    for i, (n, timing) in enumerate(zip(N_VALUES, all_timings)):
        values = [timing["timings"][k] for k in transform_keys]
        plt.bar(x + i * width, values, width=width, label=f"N={n}")
    plt.xticks(x + width * 1.5, transform_keys, rotation=30, ha="right")
    plt.ylabel("Tiempo (s)")
    plt.title("Tiempo por transformacion segun numero de particiones")
    plt.legend()
    plt.tight_layout()
    plt.savefig(f"{results_dir}/fig_time_per_transform.png", dpi=300)
    plt.close()

    print(f"\nFiguras y CSV guardados en {results_dir}/")
    print("Nota (Gustafson): si el tamaño del dataset creciera junto con N en vez de "
          "mantenerse fijo (600k filas para cualquier N), el speedup escalado seria mayor "
          "al aqui reportado, porque la fraccion serial (1-p) no crece con el tamaño del "
          "problema mientras que la porcion paralela si lo hace -- ver discusion en el documento.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="../../data/processed/incidents")
    parser.add_argument("--results-dir", default="../results")
    args = parser.parse_args()

    import os
    os.makedirs(args.results_dir, exist_ok=True)

    all_timings = run_all(args.data, args.results_dir)
    analyze(all_timings, args.results_dir)


if __name__ == "__main__":
    main()
