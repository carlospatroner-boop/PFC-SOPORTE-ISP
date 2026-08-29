# Guion del video de tolerancia a fallos (D2.2, Módulo D)

**Duración objetivo: 4-5 minutos, con audio explicativo.** Exigido por la rúbrica: video ≤5 min,
mostrando (a) `SELECT COUNT(*)` exitoso, (b) caída abrupta de un nodo, (c) reconsulta exitosa,
(d) reincorporación del nodo, y (e) la misma prueba con **dos** nodos caídos (pérdida de
disponibilidad esperada). Salida final: `docs/evidencias/tolerancia_fallos.mp4` +
`docs/evidencias/tolerancia_fallos.md` (bitácora).

---

## 0. Antes de grabar (una sola vez, no sale en el video)

### 0.1 Verificar/completar el volumen de datos (≥100,000 filas)

El Módulo D exige poblar "la tabla de mayor cardinalidad con al menos 10⁵ filas". Ahora mismo
`tickets` tiene ~20,000 filas — **falta cargar más antes de grabar**.

```bash
docker exec roach1 cockroach sql --insecure -e "SELECT count(*) FROM ticket_db.tickets;"
```

Si es menor a 100,000, cargar el archivo `db-cluster/scripts/seed_partitioned_batched.sql`
(versión en 8 lotes de 10,000 filas — **no usar `seed_partitioned.sql` directo**, esa fue la causa
del incidente de memoria documentado en `docker-compose.cockroach.yml`). Ejecutar el archivo
completo de una vez es seguro porque cada lote es su propia transacción:

```bash
docker cp db-cluster/scripts/seed_partitioned_batched.sql roach1:/seed_batched.sql
docker exec roach1 cockroach sql --insecure -f /seed_batched.sql
docker exec roach1 cockroach sql --insecure -e "SELECT count(*) FROM ticket_db.tickets;"
```

Debe quedar en ~100,000 o más.

### 0.2 Verificar que el clúster está sano y solo

```bash
docker compose -f db-cluster/docker-compose.cockroach.yml up -d
docker exec roach1 cockroach node status --insecure
```

Los tres nodos deben aparecer `is_live=true`, `is_available=true`. Cerrar cualquier otro programa
pesado (Spark, IDEs con builds corriendo) para que el "ruido del sistema anfitrión" no distorsione
la demo — es una de las amenazas a la validez que ya documentamos en el informe.

### 0.3 Preparar las ventanas en pantalla

Para que se vea todo sin cambiar de ventana constantemente, organizar 3 paneles visibles a la vez:

1. **Consola web de CockroachDB** — `http://localhost:8083` (nodo 1), pestaña "Overview" con el
   estado de los 3 nodos.
2. **Terminal A** — donde corre `load_write.py` (la carga sostenida).
3. **Terminal B** — donde se ejecutan los `docker kill` / `docker start` y las consultas
   `SELECT COUNT(*)`.

### 0.4 Instalar dependencia de `load_write.py` si falta

```bash
pip install psycopg2-binary
```

---

## 1. Guion de grabación — Prueba 1: caída de un nodo (~2:30 min)

| Tiempo | Acción en pantalla | Qué decir (narración) |
|---|---|---|
| 0:00 | Consola web visible, 3 nodos en verde | *"Este es nuestro clúster de CockroachDB, tres nodos, roach1, roach2 y roach3, los tres activos y disponibles."* |
| 0:10 | Terminal B: `docker exec roach1 cockroach sql --insecure -e "SELECT count(*) FROM ticket_db.tickets;"` | *"Antes de empezar, confirmamos que la tabla de tickets tiene más de cien mil filas, cumpliendo el mínimo del protocolo."* (debe mostrar ≥100,000) |
| 0:25 | Terminal A: `python db-cluster/scripts/load_write.py --host localhost --port 26257 --duration 60 --rate 100` | *"Ahora arrancamos una carga sostenida: cien inserciones por segundo contra el clúster, durante un minuto."* |
| 0:35 | Se ve la salida de `load_write.py` imprimiendo ventanas de 10s con P50/P95 sin errores | *"Mientras esto corre sin problema, vamos a derribar uno de los tres nodos de forma abrupta."* |
| 0:45 | Terminal B: `docker kill roach2` | *"`docker kill`, no `stop` — simula una caída real, no un apagado ordenado."* |
| 0:50 | Volver a la consola web: roach2 aparece `dead`/`unavailable`, roach1 y roach3 siguen verdes | *"En la consola vemos a roach2 caído. Los otros dos nodos siguen sanos."* |
| 1:00 | Terminal A: la carga sigue corriendo, sin errores en las ventanas de 10s | *"Y lo importante: la carga sigue insertando sin errores. Con dos nodos de tres, todavía hay quórum de mayoría para Raft, así que el clúster no pierde disponibilidad de escritura."* |
| 1:20 | Terminal B: `docker exec roach1 cockroach sql --insecure -e "SELECT count(*) FROM ticket_db.tickets;"` (mientras la carga sigue) | *"Una consulta de lectura también responde con normalidad."* |
| 1:35 | Terminal B: `docker start roach2` | *"Ahora reincorporamos el nodo caído."* |
| 1:45 | Consola web: roach2 vuelve a `live`, con las réplicas subreplicadas bajando a cero | *"El nodo vuelve a unirse al clúster y Raft reconverge: las réplicas que quedaron por debajo del factor de replicación se restauran automáticamente."* |
| 2:15 | Terminal A: `load_write.py` termina (duration=60) e imprime el resumen | *"Y termina la carga sin ninguna inserción perdida durante toda la prueba."* |

**Anotar para la bitácora**: hora exacta del `docker kill`, P50/P95 de la ventana anterior al kill,
P50/P95 de la ventana durante la caída, P50/P95 tras la reincorporación, tiempo aproximado hasta
que la consola muestra 0 réplicas subreplicadas.

---

## 2. Guion de grabación — Prueba 2: caída de dos nodos (~1:30 min)

Esta es la prueba simétrica que exige el Módulo D: con **dos de tres** nodos caídos, ya no hay
quórum de mayoría (se necesitan 2 de 3 para confirmar una escritura), así que **se espera que el
clúster pierda disponibilidad de escritura** — este resultado negativo es el que hay que mostrar,
no evitarlo.

| Tiempo | Acción en pantalla | Qué decir (narración) |
|---|---|---|
| 2:20 | Terminal A: `python db-cluster/scripts/load_write.py --host localhost --port 26257 --duration 30 --rate 50` | *"Repetimos la prueba, ahora derribando dos nodos en vez de uno."* |
| 2:30 | Terminal B: `docker kill roach2` seguido de `docker kill roach3` | *"roach2 y roach3 caen casi al mismo tiempo. Solo queda roach1 en pie."* |
| 2:40 | Terminal A: las inserciones empiezan a fallar / a colgarse (timeout) | *"Y aquí se ve el cambio: las inserciones empiezan a fallar. Con un solo nodo vivo de tres, no hay mayoría posible para que Raft confirme ninguna escritura nueva."* |
| 2:55 | Terminal B: `docker exec roach1 cockroach sql --insecure -e "SELECT count(*) FROM ticket_db.tickets;"` (se cuelga o tarda mucho) | *"Incluso una simple lectura queda bloqueada esperando quórum. Esto es exactamente el comportamiento CP que describimos en el informe: CockroachDB prioriza consistencia sobre disponibilidad cuando no puede garantizar ambas."* |
| 3:10 | Terminal B: `docker start roach2` y `docker start roach3` | *"Reincorporamos los dos nodos."* |
| 3:25 | Consola web: los tres nodos vuelven a verde, `load_write.py` (si seguía corriendo) retoma inserciones exitosas | *"Y en cuanto vuelve a haber mayoría, el clúster retoma las escrituras sin intervención manual."* |
| 3:40 | Cierre a cámara/narración | *"Esto confirma en la práctica lo que predice la teoría de consenso: tolerancia a la caída de un nodo de tres, pero no de dos, exactamente el límite que fija el quórum de Raft."* |

---

## 3. Después de grabar

1. Exportar el video a MP4, verificar que dura ≤5 minutos y que el audio se escucha bien.
2. Guardarlo en `docs/evidencias/tolerancia_fallos.mp4`.
3. Completar `docs/evidencias/tolerancia_fallos.md` (bitácora) con los números reales anotados
   durante la grabación — plantilla abajo.
4. Actualizar la Sección "Cluster y su verificación" del informe LaTeX
   (`docs/latex/secciones/cluster_verificacion.tex`) reemplazando el párrafo "Estado de este
   resultado..." con los números reales y una referencia a la Figura del frame del video.
5. Exportar un frame representativo del video como PNG (por ejemplo con
   `ffmpeg -i tolerancia_fallos.mp4 -ss 00:01:00 -frames:v 1 frame_tolerancia.png`) para insertarlo
   como figura en el LaTeX.

### Plantilla para `docs/evidencias/tolerancia_fallos.md`

```markdown
# Bitácora — Verificación de tolerancia a fallos (D2.2)

Fecha de ejecución: 2026-07-__
Volumen de datos al inicio: ______ filas en `tickets`

## Prueba 1 — Caída de 1 nodo (roach2)

- Hora del `docker kill roach2`: __:__:__
- P50/P95 ventana ANTES del kill: ___ ms / ___ ms
- P50/P95 ventana DURANTE la caída: ___ ms / ___ ms
- P50/P95 ventana DESPUÉS de `docker start roach2`: ___ ms / ___ ms
- Inserciones con error durante la caída: ___ (esperado: 0)
- Tiempo hasta 0 réplicas subreplicadas tras reincorporar: ___ s

## Prueba 2 — Caída de 2 nodos (roach2 + roach3)

- Hora del segundo `docker kill`: __:__:__
- Resultado de las inserciones tras la segunda caída: ___ (esperado: errores/timeouts)
- Resultado de `SELECT COUNT(*)` durante la caída doble: ___ (esperado: bloqueado o error)
- Tiempo hasta que el clúster retoma escrituras tras reincorporar ambos nodos: ___ s

## Conclusión

[Confirmar o contrastar lo observado contra la predicción teórica de Raft: tolera 1 de 3, no 2 de 3]
```
