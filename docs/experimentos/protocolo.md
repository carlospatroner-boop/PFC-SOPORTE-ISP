# Protocolo experimental — Paso 7 / Módulo F (D4.3)

Equipo ACC — Soporte Técnico ISP. Sigue las recomendaciones de Jain [1] y Wohlin et al. [2]
para experimentación en ingeniería de software.

## 1. Hipótesis operativa

- **H1**: el pipeline paralelo con *N* cores presenta menor tiempo medio de ejecución que el
  baseline secuencial (pandas) para |D| ≥ 5·10⁵ registros.
- **H0**: no hay diferencia significativa entre el pipeline paralelo y el baseline secuencial.

## 2. Variables

| Tipo | Variable | Valores |
|---|---|---|
| Independiente | Número de cores/particiones *N* | {1, 2, 4, 8} |
| Independiente | Modo de ejecución | Spark (`local[N]`) vs. baseline secuencial (pandas, 1 proceso) |
| Dependiente | Tiempo total de ejecución *t* | segundos |
| Dependiente | Tiempo por transformación *tᵢ* | segundos (5 transformaciones: T1–T5) |

El dataset usado es el generado por `spark/src/generate_dataset.py` con semilla fija (`seed=42`,
determinista) y 600,000 filas (> 5·10⁵, cumple el umbral de H1).

## 3. Diseño experimental

Bloque completo con **r = 10 repeticiones** por nivel de *N* y por el baseline secuencial (5
niveles en total: baseline, N=1, N=2, N=4, N=8 → 50 corridas).

Se descartan la **primera y la última repetición** de cada nivel (calentamiento de JVM/caché del
sistema operativo en la primera; posible interferencia de recolección de basura o procesos en
segundo plano acumulada hacia el final), quedando **8 muestras válidas por nivel**.

Implementado en `spark/src/protocol_experiment.py` (ver también `pipeline.py` para las 5
transformaciones y `baseline.py` para el equivalente secuencial).

## 4. Análisis

- **Estadística descriptiva** por nivel: media *t̄*, desviación típica *s*.
- **Intervalo de confianza al 95%**: *t̄ ± 1.96·s/√r* (con *r*=8, muestras ya recortadas).
- **Contraste de H1**: se compara el nivel de mayor paralelismo probado (N=8) contra el baseline
  secuencial, **emparejado por índice de repetición** (misma posición en la secuencia de
  ejecución, mismo dataset).
  1. Se prueba normalidad de las diferencias emparejadas con **Shapiro-Wilk** (α=0.05).
  2. Si no se rechaza normalidad (p > 0.05): **prueba t pareada** (`scipy.stats.ttest_rel`).
  3. Si se rechaza normalidad (p ≤ 0.05): **Wilcoxon signed-rank** (`scipy.stats.wilcoxon`), la
     alternativa no paramétrica estándar para muestras pareadas cuando no se cumple normalidad.
  4. Se rechaza H0 (a favor de H1) si el p-valor de la prueba elegida es < 0.05 **y** la
     diferencia media (secuencial − paralelo) es positiva.

## 5. Amenazas a la validez

**Internas** (factores del propio experimento que podrían explicar el resultado sin que sea la
causa real):

1. **Calentamiento de la JVM**: la primera ejecución de cada nivel de Spark paga el costo de
   inicialización de clases y JIT warm-up, no solo el trabajo real — mitigado descartando la
   primera repetición.
2. **Ruido del sistema anfitrión**: el experimento corre en un contenedor Docker sobre Windows,
   compartiendo CPU con el resto de servicios del PFC (CockroachDB, Kafka, etc. si están
   corriendo simultáneamente) — se recomienda correr el protocolo con el resto de servicios
   detenidos para minimizar interferencia.
3. **Caché de sistema operativo/disco**: lecturas repetidas del mismo Parquet se benefician de
   caché de páginas tras la primera lectura, afectando de forma no uniforme a las primeras
   repeticiones de cada nivel.

**Externas** (límites de qué tan generalizable es la conclusión):

1. **Representatividad del dataset**: es sintético (ver honestidad documentada en
   `generate_dataset.py`) — los tiempos relativos entre transformaciones podrían diferir con
   datos reales de un ISP con otra distribución de texto/cardinalidad de clientes.
2. **Generalidad a otras cargas**: el pipeline concreto (filtrado + join + ventana + tipos
   temporales + clustering ML) no representa necesariamente el comportamiento de otras cargas de
   trabajo de Spark (p. ej. cargas dominadas por *shuffles* masivos o joins de tablas muy grandes
   entre sí, en vez de una tabla grande contra una dimensión pequeña).

## 6. Reproducibilidad

```bash
cd spark
docker compose -f docker-compose.spark.yml run --rm spark generate_dataset.py --rows 600000 --out ../data/processed/incidents
docker compose -f docker-compose.spark.yml run --rm spark protocol_experiment.py --data ../data/processed/incidents --reps 10 --results-dir ../results/protocol
```

Salidas en `spark/results/protocol/` (copiadas a `docs/experimentos/resultados/` para el
manuscrito): `raw.csv` (las 50 repeticiones crudas, incluidas las descartadas, con la columna
`descartada_calentamiento_enfriamiento`), `summary_stats.csv` (media/desviación/IC95% por nivel),
`stats_test.json` (prueba usada, estadístico, p-valor, conclusión), `boxplot.png` (300 dpi).

## 7. Resultados

Ejecutado el 2026-07-27 (50 corridas reales, ~2 horas en este entorno — más de los 75-90 minutos
estimados, por el costo de arranque de una `SparkSession` nueva en cada repetición). Artefactos en
`spark/results/protocol/`: `raw.csv`, `summary_stats.csv`, `stats_test.json`, `boxplot.png`,
`amdahl_fit.json`.

| Nivel | Media (s) | Desv. típica (s) | IC95% |
|---|---|---|---|
| Baseline (pandas) | 25.42 | 4.52 | [22.29, 28.55] |
| Spark `local[1]` | 235.22 | 5.58 | [231.35, 239.09] |
| Spark `local[2]` | 163.81 | 3.92 | [161.10, 166.53] |
| Spark `local[4]` | 138.92 | 5.15 | [135.35, 142.49] |
| Spark `local[8]` | 129.36 | 4.55 | [126.21, 132.51] |

**Contraste de H1**: Shapiro-Wilk sobre las diferencias emparejadas (baseline − Spark `local[8]`)
no rechaza normalidad (W=0.963, p=0.835) → prueba t pareada: t=-47.19, p≈5.02e-10, diferencia
media -103.94s. **Resultado**: no se rechaza H0 en el sentido de H1 — el baseline secuencial en
pandas fue significativamente **más rápido** que Spark incluso en `local[8]`, no al revés. Para
este volumen de datos (600,000 filas, cabe en memoria de un solo proceso), el costo fijo de
inicializar una `SparkSession` por repetición domina sobre cualquier ganancia de paralelismo real.

**Ajuste de Amdahl (escalado interno de Spark, referencia `local[1]`)**: speedup observado
S(2)=1.44, S(4)=1.69, S(8)=1.82 → p≈0.530, S(∞)≈2.13, N para 90% de S(∞) ≈ 10.1. Es decir, dentro
de la propia ejecución de Spark el paralelismo sí funciona (más núcleos → menos tiempo), solo que
partiendo de una base ~9x más lenta que pandas para este tamaño de dataset.

Ver la discusión completa de por qué el baseline ganó (costo de arranque de JVM, dataset que cabe
en memoria de un solo nodo, dominancia de la etapa de *clustering*) en la Sección "Protocolo y
resultados" del documento LaTeX (`docs/latex/secciones/protocolo_resultados.tex`).

## Referencias

[1] R. Jain, "The art of computer systems performance analysis," Wiley, 1991.
[2] C. Wohlin et al., *Experimentation in Software Engineering*. Springer, 2012.
