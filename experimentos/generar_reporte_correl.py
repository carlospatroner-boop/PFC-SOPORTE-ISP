# -*- coding: utf-8 -*-
"""
generar_reporte_correl.py -- Cuaderno de analisis reproducible para el experimento CORREL
(cierra el hueco de C4 senalado en la revision del profesor: "el paso del dato crudo a la
tabla impresa no es reproducible sin intervencion manual").

Lee experimentos/resultados/correlacion_corridas.csv (dato crudo real, no editado a mano) y
regenera, sin intervencion manual:
  1. experimentos/resultados/correlacion_analisis.json -- igual que antes, mas el contraste
     c1 vs c2 que faltaba (ver docs/latex/secciones/resultado_correl_e4.tex, seccion
     "Contraste c1 vs c2").
  2. docs/latex/secciones/generado/tabla_resultado_correl.tex -- el fragmento de tabla que
     \\input{} usa resultado_correl_e4.tex, generado desde el CSV, no escrito a mano.

Uso:
    python experimentos/generar_reporte_correl.py
"""
import csv
import json
from pathlib import Path

import numpy as np
from scipy import stats

ROOT = Path(__file__).parent.parent
CORRIDAS_CSV = ROOT / "experimentos" / "resultados" / "correlacion_corridas.csv"
ANALISIS_JSON = ROOT / "experimentos" / "resultados" / "correlacion_analisis.json"
TABLA_TEX = ROOT / "docs" / "latex" / "secciones" / "generado" / "tabla_resultado_correl.tex"

MODOS = ["c0", "c1", "c2"]


def vargha_delaney_a(x, y):
    """A de Vargha-Delaney: P(muestra de x > muestra de y), con empates a 0.5."""
    n1, n2 = len(x), len(y)
    if n1 == 0 or n2 == 0:
        return None
    u, _ = stats.mannwhitneyu(x, y, alternative="greater")
    return float(u) / (n1 * n2)


def cargar_filas():
    with open(CORRIDAS_CSV, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def contraste(nombre_a, valores_a, nombre_b, valores_b):
    """Mann-Whitney U bilateral + Vargha-Delaney A(a mayor que b), o 'identicos' si no hay
    variacion en ninguno de los dos grupos y son iguales entre si (evita el ValueError de
    scipy cuando todas las diferencias son cero)."""
    if list(valores_a) == list(valores_b) or (len(set(valores_a)) == 1 and len(set(valores_b)) == 1 and valores_a[0] == valores_b[0]):
        return {
            "prueba": "Mann-Whitney U (bilateral)",
            "estadistico_u": None,
            "p_valor": None,
            "vargha_delaney_a": 0.5,
            "identicos_valor_a_valor": True,
        }
    stat, p = stats.mannwhitneyu(valores_a, valores_b, alternative="two-sided")
    a = vargha_delaney_a(valores_a, valores_b)
    return {
        "prueba": "Mann-Whitney U (bilateral)",
        "estadistico_u": float(stat),
        "p_valor": float(p),
        "vargha_delaney_a": a,
        "identicos_valor_a_valor": False,
    }


def main():
    filas = cargar_filas()
    for r in filas:
        r["precision"] = float(r["precision"])
        r["exhaustividad"] = float(r["exhaustividad"])
        r["incidencias_efectivas"] = int(r["incidencias_efectivas"])

    por_modo = {}
    for modo in MODOS:
        precisiones = [r["precision"] for r in filas if r["modo"] == modo]
        exhaustividades = [r["exhaustividad"] for r in filas if r["modo"] == modo]
        incidencias = [r["incidencias_efectivas"] for r in filas if r["modo"] == modo]
        por_modo[modo] = {
            "n": len(precisiones),
            "precision_media": round(float(np.mean(precisiones)), 4),
            "precision_mediana": round(float(np.median(precisiones)), 4),
            "exhaustividad_media": round(float(np.mean(exhaustividades)), 4),
            "exhaustividad_mediana": round(float(np.median(exhaustividades)), 4),
            "incidencias_efectivas_media": round(float(np.mean(incidencias)), 2),
        }

    exh = {m: [r["exhaustividad"] for r in filas if r["modo"] == m] for m in MODOS}
    prec = {m: [r["precision"] for r in filas if r["modo"] == m] for m in MODOS}
    inc = {m: [r["incidencias_efectivas"] for r in filas if r["modo"] == m] for m in MODOS}

    contraste_c2_c0 = contraste("c2", exh["c2"], "c0", exh["c0"])
    # Contraste que faltaba: c1 vs c2 (la telemetria es lo unico que distingue a c2 de c1;
    # si no hay diferencia, la telemetria no aporto nada medible bajo este diseno).
    contraste_c1_c2_exhaustividad = contraste("c1", exh["c1"], "c2", exh["c2"])
    contraste_c1_c2_precision = contraste("c1", prec["c1"], "c2", prec["c2"])
    identicos_valor_a_valor = (
        exh["c1"] == exh["c2"] and prec["c1"] == prec["c2"] and inc["c1"] == inc["c2"]
    )

    esc4 = [r for r in filas if r["escenario"] == "Esc-4_dos_averias_simultaneas"]
    fusiones = sum(1 for r in esc4 if r["tickets_en_incidencia_elegida"] > r["tickets_averia"]
                   and r["interseccion"] < r["tickets_en_incidencia_elegida"])
    n_esc4 = len(esc4)
    p_hat = fusiones / n_esc4 if n_esc4 else 0.0
    se = (p_hat * (1 - p_hat) / n_esc4) ** 0.5 if n_esc4 else 0.0
    ic_lo, ic_hi = max(0.0, p_hat - 1.96 * se), min(1.0, p_hat + 1.96 * se)

    analisis = {
        "por_modo": por_modo,
        "contraste_c2_vs_c0_exhaustividad": contraste_c2_c0,
        "contraste_c1_vs_c2_exhaustividad": contraste_c1_c2_exhaustividad,
        "contraste_c1_vs_c2_precision": contraste_c1_c2_precision,
        "c1_y_c2_identicos_valor_a_valor": identicos_valor_a_valor,
        "fusion_erronea_esc4": {
            "n_corridas": n_esc4, "fusiones_observadas": fusiones,
            "ic95_binomial": [round(ic_lo, 4), round(ic_hi, 4)],
        },
    }

    with open(ANALISIS_JSON, "w", encoding="utf-8") as f:
        json.dump(analisis, f, indent=2, ensure_ascii=False)
    print(f"Analisis regenerado en {ANALISIS_JSON}")
    print(json.dumps(analisis, indent=2, ensure_ascii=False))

    # ---- Fragmento de tabla LaTeX, generado, no escrito a mano ----
    TABLA_TEX.parent.mkdir(parents=True, exist_ok=True)
    filas_tabla = "\n".join(
        f"\\texttt{{{m}}} ({desc}) & {por_modo[m]['precision_media']:.2f} & "
        f"{por_modo[m]['exhaustividad_media']:.2f} & {por_modo[m]['incidencias_efectivas_media']:.1f} \\\\"
        for m, desc in [
            ("c0", "sin correlaci\\'on"), ("c1", "zona+ventana"), ("c2", "zona+ventana+telemetr\\'ia"),
        ]
    )
    tabla = (
        "% Generado automaticamente por experimentos/generar_reporte_correl.py -- NO EDITAR A MANO.\n"
        "% Fuente: experimentos/resultados/correlacion_corridas.csv (60 filas reales).\n"
        "\\begin{table}[H]\n\\centering\n"
        "\\caption{Precisi\\'on y exhaustividad del agrupamiento por configuraci\\'on CORREL "
        f"(n={por_modo['c0']['n']} por configuraci\\'on)}}\n"
        "\\label{tab:resultado-correl}\n\\begin{tabular}{@{}lrrr@{}}\n\\toprule\n"
        "\\textbf{Configuraci\\'on} & \\textbf{Precisi\\'on} & \\textbf{Exhaustividad} & "
        "\\textbf{Incidencias efectivas (media)} \\\\\n\\midrule\n"
        + filas_tabla + "\n\\bottomrule\n\\end{tabular}\n\\end{table}\n"
    )
    TABLA_TEX.write_text(tabla, encoding="utf-8")
    print(f"\nTabla LaTeX regenerada en {TABLA_TEX}")


if __name__ == "__main__":
    main()
