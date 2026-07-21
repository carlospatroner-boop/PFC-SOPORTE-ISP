# spark — Pipeline de procesamiento paralelo (equipo ACC)

## Correrlo con Docker (recomendado — igual en Windows/Mac/Linux)

PySpark en Windows requiere `winutils.exe`/librerías nativas de Hadoop que no vienen por
defecto y son frágiles de instalar (binarios no oficiales, distintos por versión). Para que
el pipeline corra igual en la máquina de cualquier integrante del equipo (y el día de la
defensa, sea cual sea la laptop), usamos un contenedor Linux con Java 21 + PySpark ya
instalados (`Dockerfile` en esta carpeta).

Requisitos: Docker Desktop, y el cluster de `db-cluster/` ya levantado (comparten la red
`soporte-net`, necesaria para el paso 5 de integración JDBC).

```bash
cd spark
docker compose -f docker-compose.spark.yml build
```

Los tres pasos siguientes se corren igual, sustituyendo `python archivo.py` por
`docker compose -f docker-compose.spark.yml run --rm spark archivo.py`.

**Nota (Paso 3 — Amdahl):** para que el punto N=8 sea representativo, asignar al menos
8 vCPUs a Docker Desktop en Settings > Resources.

### Alternativa: instalar PySpark localmente (Linux/Mac sin fricción, Windows con `winutils.exe`)

```bash
pip install pandas numpy pyarrow pyspark scipy matplotlib --break-system-packages
```

Requiere Java 11+ instalado (PySpark lo necesita aunque el resto del stack sea Java 21). En
Windows hace falta además configurar `HADOOP_HOME` con `winutils.exe`/`hadoop.dll` — por eso
se recomienda la vía Docker de arriba.

Todos los comandos de abajo se corren desde esta carpeta (`spark/`), no desde `spark/src/`.

## 1. Generar el dataset (≥500,000 filas)

```bash
docker compose -f docker-compose.spark.yml run --rm spark generate_dataset.py --rows 600000 --out ../data/processed/incidents
```

Esto crea Parquet particionado por zona en `spark/data/processed/incidents/`. Documentar en
`spark/data/README.md` que el dataset es sintético y por qué (ver comentario en el propio script).

## 2. Validar el pipeline una vez (antes de medir speedup)

```bash
docker compose -f docker-compose.spark.yml run --rm spark pipeline.py --data ../data/processed/incidents --master "local[4]"
```

Debe imprimir los tiempos de las 5 transformaciones y la tabla de "zonas problemáticas".

## 3. Medir speedup y ajustar Amdahl (Paso 7)

```bash
docker compose -f docker-compose.spark.yml run --rm spark amdahl_analysis.py --data ../data/processed/incidents --results-dir ../results
```

Esto corre el pipeline completo 4 veces (N=1,2,4,8), y en `spark/results/` deja:
- `speedup_table.csv` — tabla para el documento LaTeX.
- `fig_speedup.png` y `fig_time_per_transform.png` — figuras a 300 dpi.
- `amdahl_fit.json` — p ajustado, S(∞), y N necesario para el 90% de S(∞) (pregunta 3 del Anexo B).

## 4. Exportar el notebook

Para cumplir con el requisito de `notebooks/spark_pipeline.ipynb` + `.html`, convertir estos
scripts a un notebook de Jupyter (celda por transformación) y exportarlo:

```bash
pip install jupyter nbconvert --break-system-packages
jupyter nbconvert --to html notebooks/spark_pipeline.ipynb
```

## 5. Integración con el PFC (Paso 8)

El resultado de `t4_mttr_by_zone_hour` / `t5_problematic_zones` debe escribirse como tabla en
CockroachDB (`network_incidents_summary`, ya creada en `db-cluster/scripts/init_db.sql`) usando el
conector JDBC de Spark:

```python
result_df.write.jdbc(
    url="jdbc:postgresql://roach1:26257/ticket_db?sslmode=disable",
    table="network_incidents_summary",
    mode="append",
    properties={"user": "root", "driver": "org.postgresql.Driver"},
)
```

Nota: la URL usa `roach1` (el hostname del contenedor), no `localhost` — el contenedor de Spark
y el cluster comparten la red Docker `soporte-net`, así que se ven entre sí por nombre de
servicio, no por `localhost` del host. Si corres este paso con PySpark instalado localmente
(fuera de Docker) en vez de con el contenedor de Spark, usa `localhost:26257` en su lugar.

(Requiere el JAR de `postgresql` en el classpath de Spark — CockroachDB es compatible con el
protocolo/driver de PostgreSQL. Aún no está agregado al `Dockerfile` de `spark/`; hace falta
sumarlo cuando se implemente este paso.)
