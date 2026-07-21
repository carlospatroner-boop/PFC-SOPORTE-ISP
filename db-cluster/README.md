# db-cluster — Cluster CockroachDB del PFC (equipo ACC)

## Cómo levantar el cluster

```bash
cd db-cluster
docker compose -f docker-compose.cockroach.yml up -d
docker exec -it roach1 cockroach init --insecure
```

Verificar en la consola web que los 3 nodos aparecen `live`: http://localhost:8080

## Cargar el esquema

```bash
cockroach sql --insecure --host=localhost:26257 -f scripts/init_db.sql
cockroach sql --insecure --host=localhost:26257 -f config/zones.sql
cockroach sql --insecure --host=localhost:26257 -f scripts/seed_partitioned.sql
```

Verificar la partición:

```sql
SHOW PARTITIONS FROM TABLE ticket_db.tickets;
SELECT zone, count(*) FROM ticket_db.tickets GROUP BY zone;
```

## Prueba de tolerancia a fallos (Paso 4)

Terminal 1 (carga sostenida):
```bash
pip install psycopg2-binary --break-system-packages
python scripts/load_write.py --duration 180 --rate 100
```

Terminal 2 (a mitad de la ejecución, ~90s después de iniciar):
```bash
docker stop roach2
# esperar 30-60s, observar que la carga sigue sin errores
docker start roach2
```

Grabar en simultáneo: consola web (8080), terminal 1 y el comando `docker stop`. Guardar el
video en `docs/evidencias/fault_tolerance.mp4` y un frame representativo en
`docs/evidencias/fault_tolerance.png`.

## Comparativa de rendimiento (Paso 5)

```bash
# Cluster de 3 nodos ya levantado arriba
cockroach sql --insecure --host=localhost:26257 -f scripts/queries_bench.sql > results_cluster.txt

# Nodo único para comparar
cockroach start-single-node --insecure --listen-addr=localhost:26260 --http-addr=localhost:8090 &
cockroach sql --insecure --host=localhost:26260 -f scripts/init_db.sql
cockroach sql --insecure --host=localhost:26260 -f scripts/seed_partitioned.sql
cockroach sql --insecure --host=localhost:26260 -f scripts/queries_bench.sql > results_single_node.txt
```

Comparar los tiempos de `EXPLAIN ANALYZE` de ambos archivos en la tabla del documento LaTeX.
