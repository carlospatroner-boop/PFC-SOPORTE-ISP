# ADR-0003: Política de fragmentación horizontal por zona geográfica

## Estado
Aceptado — Entrega 3 (Junio 2026)

## Contexto
El sistema de gestión de solicitudes de soporte técnico de Internet (equipo ACC) opera sobre
varias zonas de cobertura del ISP (definidas en la Entrega 2 como parte del dominio `ticket-service`).
El volumen de tickets crece de forma proporcional al número de abonados por zona, y la mayoría de
las consultas operativas (dashboard de técnicos, SLA, asignación) se filtran por zona antes que por
cualquier otro criterio. Con una tabla `tickets` monolítica, todas las escrituras y lecturas compiten
por los mismos rangos de claves, y no hay forma de anclar la carga de una zona a un subconjunto de
nodos sin fragmentar explícitamente los datos.

Adicionalmente, el docente exige (Fundamento teórico exigible, sección 3.2) que la fragmentación se
justifique con las tres condiciones de corrección de Özsu & Valduriez [1]: completitud (todo ticket
pertenece a exactamente una partición), reconstrucción (la unión de las particiones reconstruye la
tabla completa mediante `UNION ALL` sin pérdida) y disyunción (las particiones no se solapan, ya que
`zone` es un valor discreto y excluyente).

## Decisión
Se aplica **fragmentación horizontal por LIST** sobre la columna `zone` de la tabla `tickets`,
usando `PARTITION BY LIST (zone)` de CockroachDB, con tres particiones nombradas (`tickets_centro`,
`tickets_norte`, `tickets_sur`) más una partición `DEFAULT` de resguardo para zonas no anticipadas.
La columna `zone` se incorpora como primer componente de la clave primaria compuesta `(zone, id)`,
requisito técnico de CockroachDB para que el particionamiento sea efectivo.

Cada partición se ancla preferentemente (mediante `CONFIGURE ZONE ... constraints`) al nodo cuya
`locality` coincide con la zona operativa correspondiente, aunque se mantienen 3 réplicas por
partición (una en cada nodo) para no comprometer la tolerancia a fallos: la pérdida de cualquier
nodo individual no debe dejar una zona sin servicio.

Se descartó la fragmentación vertical (separar columnas de `tickets` en tablas distintas) porque el
patrón de acceso del dominio lee casi siempre la fila completa del ticket, y la fragmentación
vertical solo aporta valor cuando hay columnas de acceso muy infrecuente o de gran tamaño (blobs),
que no es el caso aquí.

## Consecuencias

**Positivas:**
- Las consultas filtradas por zona (la mayoría del tráfico real) se resuelven consultando
  preferentemente los rangos anclados a un nodo, reduciendo la coordinación entre nodos.
- El análisis de sesgo (Paso 3) es predecible: la distribución de abonados por zona determina
  directamente el tamaño de cada partición, sin necesidad de una función hash que oculte la
  relación entre dato y ubicación física.
- Facilita la respuesta a la pregunta obligatoria del Anexo B sobre hot-spots: como cada zona
  crece a un ritmo similar (no hay una zona que concentre >80% del tráfico), no se anticipan
  puntos calientes severos: se documentará con las estadísticas reales de `seed_partitioned.sql`.

**Negativas / riesgos:**
- Las consultas que cruzan zonas (por ejemplo, un reporte global de SLA en todo el ISP) requieren
  un `scatter-gather` entre las tres particiones, con mayor latencia que una consulta de una sola
  zona. Se documenta en el análisis comparativo (Paso 5, consulta Q3).
- Si en el futuro el ISP se expande a una cuarta zona, requiere una migración de esquema
  (añadir partición) que debe planificarse; se mitiga con la partición `DEFAULT` como resguardo
  temporal.

## Referencias
[1] M. T. Özsu and P. Valduriez, *Principles of Distributed Database Systems*, 4th ed. Springer, 2020.
[2] Cockroach Labs, "Partitioning" — CockroachDB technical documentation, 2024.
