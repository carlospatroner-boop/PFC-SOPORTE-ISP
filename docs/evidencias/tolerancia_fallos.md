# Bitácora — Verificación de tolerancia a fallos (D2.2)

**Fecha de ejecución**: 2026-07-27 (grabación real, `docs/evidencias/tolerancia_fallos.mp4`, 4:55 min)
**Volumen de datos al inicio**: 101,996 filas en `ticket_db.tickets`
(`docker exec roach1 cockroach sql --insecure -e "SELECT count(*) FROM ticket_db.tickets;"`)

Todos los números de esta bitácora se extrajeron directamente de la terminal visible en el video
(capturas de pantalla en `docs/evidencias/frames/`), no de un ensayo previo ni de una estimación.

## Prueba 1 — Caída de 1 nodo (roach2)

Carga: `python db-cluster/scripts/load_write.py --host localhost --port 26257 --duration 60 --rate 100`

| Ventana (fin, UTC) | P50 | P95 | Errores | Filas |
|---|---|---|---|---|
| 01:32:22 | 38.6 ms | 118.4 ms | 0 | 110 |
| **01:32:32** | **69.3 ms** | **1590.8 ms** | 0 | **44** |
| 01:32:44 | 31.0 ms | 107.9 ms | 0 | 130 |
| 01:32:54 | 40.2 ms | 95.7 ms | 0 | 114 |
| 01:33:04 | 26.3 ms | 69.3 ms | 0 | 163 |

**Total insertado**: 691 filas. **Errores totales**: 0.

- `docker kill roach2` se ejecutó durante la ventana marcada en negrita: el P95 se dispara a
  1590.8 ms (25× el valor normal) y el conteo de filas de esa ventana cae a 44 (vs. ~110-160 en el
  resto), consistente con el tiempo que tomó a Raft reelegir líder para los rangos cuyo
  *leaseholder* era roach2.
- **Cero errores en todo el experimento**: ninguna inserción se perdió ni falló; el único efecto
  observable de la caída de un nodo fue latencia elevada y transitoria, no indisponibilidad.
- Recuperación completa desde la ventana siguiente (P95 vuelve a rango normal, 69-120 ms).
- `docker start roach2` se ejecutó inmediatamente después de confirmar la recuperación.

## Prueba 2 — Caída de 2 nodos (roach2 + roach3)

Carga: `python db-cluster/scripts/load_write.py --host localhost --port 26257 --duration 30 --rate 50`

Secuencia real de comandos (Terminal B, en orden): `docker kill roach2` → `docker kill roach3`
(con roach2 ya recuperado de la Prueba 1, esto deja **solo roach1 en pie**) → se intenta
`SELECT count(*) FROM ticket_db.tickets` → `docker start roach2`.

| Ventana (fin, UTC) | P50 | P95 | Errores | Filas |
|---|---|---|---|---|
| 01:34:15 | 33.0 ms | 82.7 ms | 0 | 129 |
| 01:35:17 | 20.6 ms | 43.9 ms | 0 | 204 |

**Total insertado**: 333 filas. **Errores totales**: 0.

- Entre el fin de la primera ventana (01:34:15) y el fin de la segunda (01:35:17) hay una brecha de
  **62 segundos**, muy por encima de los ~10-12 s observados en cualquier otra ventana de ambas
  pruebas. Esta brecha coincide con la ejecución de `docker kill roach2` (segunda vez) y
  `docker kill roach3`: con solo roach1 vivo de tres, no existe mayoría para que Raft confirme
  escrituras sobre los rangos cuyo líder estaba en roach2 o roach3, así que la conexión abierta de
  `load_write.py` quedó bloqueada esperando esa confirmación en vez de recibir un error inmediato.
- La consulta `SELECT count(*)` lanzada durante esta ventana de dos nodos caídos no devolvió
  resultado visible antes de que se decidiera reincorporar `roach2` — comportamiento consistente
  con el bloqueo esperado por falta de quórum, no con un error explícito del cliente.
- El script `load_write.py` no reporta errores explícitos (0 en ambas ventanas) porque su única
  conexión permaneció abierta contra roach1 (que nunca cayó) y esperó en vez de fallar; el efecto
  observable de la pérdida de quórum fue **indisponibilidad por bloqueo** (62 s sin progreso), no
  una excepción — ambos son formas válidas de "pérdida de disponibilidad de escritura", la
  diferencia depende del comportamiento del cliente/driver, no del clúster.
- La recuperación ocurre en cuanto se ejecuta `docker start roach2`: con roach1 + roach2 vivos
  vuelve a haber mayoría (2 de 3), la solicitud pendiente se completa, y la ventana siguiente
  (01:35:17) ya muestra latencia normal (20.6/43.9 ms).

## Conclusión

Ambas pruebas confirman en la práctica la predicción teórica de Raft aplicada a un factor de
replicación 3: el clúster **tolera la caída de un nodo** sin pérdida de disponibilidad de
escritura (solo latencia elevada y transitoria durante la reelección de líder), pero **pierde
disponibilidad de escritura ante la caída de dos de tres nodos** hasta que se restaura el quórum
de mayoría — exactamente el límite que predice el algoritmo de consenso
(Ongaro y Ousterhout, 2014) y que se documentó en el marco teórico de este trabajo.

## Evidencia

- Video completo: `docs/evidencias/tolerancia_fallos.mp4` (4:55 min).
- Capturas de la terminal usadas para esta bitácora: `docs/evidencias/frames/`.
- Consola web de CockroachDB tras la recuperación final: los 3 nodos (`quevedo-centro`,
  `quevedo-sur`, `quevedo-norte`) muestran estado `LIVE` con 61 réplicas cada uno.
