# ADR-0003: Política de fragmentación horizontal por fecha de apertura

## Estado
Aceptado — Entrega 3 (Julio 2026). Reemplaza la decisión original de fragmentar por
zona geográfica, tras la publicación de la Guía Integral de la Entrega 3 (v1.0), cuya
Tabla 1 exige explícitamente para el equipo ACC: *"Fragmentación horizontal de
`tickets` por `fecha_apertura`; colocalización clientes ⋈ tickets por `cliente_id`"*.

## Contexto
La primera versión de este ADR fragmentaba `tickets` por zona geográfica
(`PARTITION BY LIST (zone)`), justificada por el hecho de que la mayoría de las
consultas operativas (dashboard de técnicos, asignación) se filtran por zona. Esa
decisión sigue siendo defendible desde el punto de vista del patrón de acceso, pero
la rúbrica oficial de la Entrega 3 exige, de forma explícita y no negociable para
este equipo, fragmentación por `fecha_apertura` — el equivalente temporal usado
también por el resto de equipos del curso (ej. AGLS particiona `pedidos` por
`fecha_pedido` con `PARTITION BY RANGE` trimestral). Se prioriza el cumplimiento
literal del enunciado sobre la preferencia de diseño original.

El docente exige además (Fundamento teórico, sección 4.1) que la fragmentación
cumpla las tres condiciones de corrección de Özsu & Valduriez [1]:
- **Completitud**: todo ticket tiene un `created_at` no nulo, por lo que pertenece
  a exactamente una partición trimestral.
- **Reconstrucción**: la unión de las 4 particiones (`UNION ALL`) reconstruye la
  tabla completa sin pérdida, porque los rangos `[MINVALUE, 2026-04-01)`,
  `[2026-04-01, 2026-07-01)`, `[2026-07-01, 2026-10-01)` y `[2026-10-01, MAXVALUE)`
  son colectivamente exhaustivos.
- **Disyunción**: los rangos no se solapan (cada extremo es exclusivo por la
  izquierda e inclusivo por la derecha, semántica estándar de `PARTITION BY RANGE`
  en CockroachDB), por lo que ningún ticket puede pertenecer a dos particiones.

## Decisión
Se aplica **fragmentación horizontal por RANGE** sobre la columna `created_at` de
la tabla `tickets`, con 4 particiones trimestrales para 2026
(`tickets_2026_q1`..`tickets_2026_q4`). La columna `created_at` pasa a ser el
primer componente de la clave primaria compuesta `(created_at, id)` — requisito
técnico de CockroachDB para que el particionamiento por rango sea efectivo (ver
`db-cluster/scripts/init_db.sql`).

Como `id` deja de ser autosuficiente como clave de acceso (la aplicación no conoce
`created_at` de antemano cuando solo tiene el UUID del ticket), se añade un
**índice único secundario** `tickets_id_key` sobre `id`, que es el que usa
`TicketRepository.findByTicketId(UUID)` para resolver el punto de acceso real del
sistema (`GET/PATCH/POST /api/v1/tickets/{id}...`) sin tener que escanear las 4
particiones.

**Sobre la colocalización clientes ⋈ tickets por `cliente_id`:** la guía asume un
esquema donde ambas tablas viven en la misma base de datos, lo que permitiría usar
`INTERLEAVE IN PARENT` o una clave compuesta compartida. En la arquitectura de
microservicios de este PFC (heredada de la Entrega 2), los clientes son propiedad
de `auth-service` (`auth_db.users`), una base de datos **físicamente distinta** a
`ticket_db` — cada microservicio es dueño de su propio esquema, sin acceso directo
cruzado. Colocalizar ambas tablas en el sentido literal de CockroachDB
(`INTERLEAVE`/clave compartida en la misma base) violaría ese límite de propiedad
de datos entre servicios, que es una decisión arquitectónica deliberada de E2, no
un descuido. La aproximación equivalente adoptada aquí es: `tickets.client_id` se
mantiene indexado (`idx_tickets_zone` cubre `zone`; un índice análogo sobre
`client_id` respalda `findByClientId`/`findByClientIdAndStatus`), y la resolución
de identidad del cliente ocurre a nivel de aplicación vía el JWT emitido por
auth-service, no a nivel de almacenamiento físico compartido. Se documenta esta
tensión explícitamente en vez de forzar una colocalización artificial que
comprometería la separación de servicios.

**Sobre la zona geográfica:** `zone` no desaparece del modelo -- sigue siendo una
columna real de la tabla, respaldada por el índice secundario `idx_tickets_zone`,
y sigue sustentando el RBAC de TECNICO (un técnico solo ve/gestiona tickets de su
propia zona, ver `TicketService`) y los reportes de `report-service`. Lo que
cambia es que `zone` ya **no es la clave de fragmentación física**: una consulta
filtrada por zona ahora cruza las 4 particiones trimestrales (scatter-gather),
mientras que antes se resolvía dentro de una sola partición. Este trade-off se
documenta más abajo y se mide en `db-cluster/scripts/queries_bench.sql` (Q3).

Se descartó la fragmentación vertical por el mismo motivo que en la versión
anterior de este ADR: el patrón de acceso del dominio lee casi siempre la fila
completa del ticket.

## Consecuencias

**Positivas:**
- Cumple literalmente el requisito de la rúbrica oficial de E3 para el equipo ACC.
- Las consultas de rango temporal (la mayoría de los reportes: "tickets abiertos
  hoy", "tickets del último trimestre") se resuelven dentro de una o dos
  particiones contiguas, sin coordinación entre las 4.
- El ciclo de vida de los datos se vuelve natural: una partición trimestral vieja
  puede archivarse o reconfigurarse con una política de replicación distinta
  (menos réplicas, almacenamiento más frío) sin tocar las particiones activas —
  un patrón habitual en sistemas de series temporales que la fragmentación por
  zona no ofrecía.

**Negativas / riesgos:**
- Las consultas filtradas por zona (el filtro más frecuente del dominio real de
  un ISP: "tickets de mi zona", RBAC de TECNICO) ya no se benefician de poda de
  particiones y cruzan las 4 particiones trimestrales. Se documenta y mide en el
  análisis comparativo (Q3 de `queries_bench.sql`).
- El punto de acceso más común del sistema (buscar un ticket por id) ya no cae
  directamente en la clave primaria; requiere el salto adicional del índice
  secundario `tickets_id_key`. Se acepta este costo porque es un único salto
  índice→tabla, mucho menor que un scatter-gather sobre las 4 particiones.
- Al cierre de cada año calendario hace falta una migración de esquema para
  añadir las particiones del año siguiente (`ALTER TABLE ... ADD PARTITION`),
  que debe planificarse con antelación.

## Referencias
[1] M. T. Özsu and P. Valduriez, *Principles of Distributed Database Systems*, 4th ed. Springer, 2020.
[2] Cockroach Labs, "Partitioning" — CockroachDB technical documentation, 2024.
