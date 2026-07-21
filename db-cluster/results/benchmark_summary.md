# Comparativa de rendimiento — clúster (3 nodos) vs. nodo único

**Fecha:** 2026-07-18
**Dataset:** `ticket_db.tickets` (20 000 filas) + `technicians` (6 filas), idéntico en ambos entornos.
**Metodología:** cada una de las 5 consultas de [`scripts/queries_bench.sql`](../scripts/queries_bench.sql)
se ejecutó con `EXPLAIN ANALYZE` 5 veces seguidas en cada entorno, tras correr `ANALYZE` sobre las
tablas (para evitar el sesgo de "missing stats" del primer query tras la carga masiva). Se reporta la
**mediana** de las 5 corridas para neutralizar outliers de arranque en frío y de contención de la VM de
Docker Desktop/WSL2 (ver nota de entorno más abajo). Datos crudos de cada corrida en
[`resultados.csv`](resultados.csv).

| # | Consulta | Descripción | Cluster (mediana) | Nodo único (mediana) | Factor mejora* | Interpretación |
|---|---|---|---|---|---|---|
| Q1 | Lectura por PK | `WHERE zone=... AND id=(subconsulta)` — punto de acceso único | 4 ms | 2 ms | 0.5× | El nodo único gana: un lookup de una sola fila no tiene trabajo que paralelizar, así que el único costo relevante es la coordinación entre nodos del clúster (aunque sea local), que el nodo único no paga. |
| Q2 | Rango por zona + fecha | Filtro por partición + `created_at`, `ORDER BY` | 7 ms | 11 ms | 1.6× | El clúster gana: usa el índice `idx_tickets_created_at` y la localidad de la partición para resolver el rango sin escanear las otras dos zonas. |
| Q3 | Agregación cruzando particiones | `GROUP BY zone` sobre ventana de 7 días, toca las 3 particiones | 13 ms | 14 ms | 1.1× | Prácticamente empatados a este volumen de datos; el plan del clúster distribuye el `scan` en `n1` y ejecuta en paralelo, compensando el costo de cruzar particiones. |
| Q4 | `GROUP BY` zona+categoría (SLA breach) | Full scan + agregación con `CASE`, la más pesada de las 5 | 26 ms | 34 ms | 1.3× | El clúster gana con margen: es la consulta con más trabajo de cómputo (agregación sobre 20 000 filas), y ahí el plan distribuido (`nodes: n1, n2`) sí aporta paralelismo real. |
| Q5 | `JOIN` tickets↔técnicos | Filtro por status + límite 500 | 28 ms | 30 ms | 1.1× | Comparable; el `hash join`/`lookup join` es económico en ambos casos porque `technicians` es una tabla pequeña (6 filas). |

*factor de mejora = tiempo_nodo_único / tiempo_cluster (>1 significa que el clúster fue más rápido).

## Conclusión cualitativa

A este volumen (20 000 filas), la ventaja de rendimiento del clúster de 3 nodos frente al nodo único es
**modesta y depende del tipo de consulta**: gana claramente solo en la consulta más pesada en cómputo
(Q4), empata en las de rango/agregación cruzada (Q2, Q3, Q5), y pierde en el lookup puntual por clave
primaria (Q1), donde el costo de coordinación distribuida no tiene contrapartida en paralelismo real que
compensarlo. Esto es consistente con la teoría: la fragmentación y replicación distribuida rinden sobre
todo cuando el volumen de datos y el trabajo por consulta crecen lo suficiente como para que el
paralelismo entre nodos supere el costo fijo de coordinación (Raft, resolución de rangos, gRPC entre
nodos) — algo a discutir explícitamente en el análisis crítico del documento LaTeX, con referencia al plan
de `EXPLAIN ANALYZE` de cada consulta.

## Nota de entorno (limitación metodológica, declarar en el documento)

Las mediciones se corrieron en un entorno de desarrollo local (Windows 11 + Docker Desktop sobre
WSL2), no en hardware dedicado. Se observaron dos fuentes de ruido documentadas honestamente aquí:

1. **Outliers de arranque en frío / mantenimiento en segundo plano:** algunas corridas individuales
   mostraron picos aislados (Q1 clúster: 778 ms en la corrida 3; Q4 nodo único: 1600 ms en la corrida 2)
   atribuibles a jobs internos de CockroachDB (recolección de estadísticas, compactación de Pebble tras
   la carga masiva) compitiendo por CPU/disco. Por eso se reporta la mediana de 5 corridas y no una sola
   muestra.
2. **`--max-offset` elevado a 4500 ms** (por defecto 500 ms) en ambos entornos, porque el reloj de la VM
   de WSL2 se desincroniza varios segundos después de que el host sale de suspensión, lo cual impedía
   que CockroachDB arrancara. Ver comentario en
   [`docker-compose.cockroach.yml`](../docker-compose.cockroach.yml). Esto no afecta los tiempos de
   ejecución medidos (solo la tolerancia de arranque), pero debe declararse como decisión de entorno de
   desarrollo, no de producción.
