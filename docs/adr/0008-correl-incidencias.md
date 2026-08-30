# ADR-0008: Agrupamiento de tickets en Incidencias (variable `CORREL`)

## Estado
Aceptado — Entrega 4 (Agosto 2026)

## Contexto
La Adición 1 de la Ampliación del Módulo G (guía de reutilización) pide agregar una variable de
entorno `CORREL` que conmute entre tres estrategias de correlación (`c0`/`c1`/`c2`), "en el
punto donde hoy se crea la incidencia a partir de un ticket". Se verificó contra el código real:
**ese punto no existe** — no hay ningún concepto de "Incidencia" (agrupación de tickets
relacionados con la misma avería) en `ticket-service`, en ningún commit. La Adición 1 no era
"agregar un condicional a algo ya construido": había que construir la agrupación desde cero, y
recién sobre eso agregar el switch de estrategia — mismo patrón que PE-U1 (ver ADR-0007).

## Decisión
Se introduce `domain/Incidencia.java` (POJO de dominio puro, sin JPA, mismo estilo que
`Ticket.java`) y el patrón Strategy `domain/correlation/CorrelationStrategy.java`, con tres
implementaciones, cada una registrada con el nombre de bean literal `c0`/`c1`/`c2` para que
Spring resuelva la activa directamente desde el valor de `CORREL` (inyectando un
`Map<String, CorrelationStrategy>` en `CorrelationService`):

1. **`c0` (`SinCorrelacionStrategy`)** — línea base: cada ticket abre su propia incidencia.
2. **`c1` (`ZonaVentanaStrategy`)** — se une a la incidencia abierta más reciente de la misma
   zona dentro de una ventana deslizante (`correlation.window-minutes`, default 15 min); si no
   hay ninguna, abre una nueva. Deliberadamente ciega a la telemetría.
3. **`c2` (`ZonaVentanaTelemetriaStrategy`)** — mismo criterio de zona+ventana que `c1`, más una
   consulta real por gRPC a `telemetry-service` (PE-U1, ver ADR-0007) pidiendo los eventos de
   esa zona en la ventana. Solo agrupa si hay al menos un evento `EQUIPO` real que corroborar;
   si no hay evidencia (o el canal está caído), el ticket queda aislado en su propia incidencia.

El enganche ocurre en `CreateTicketHandler`, justo después de persistir el ticket, envuelto en
`try/catch` dentro de `CorrelationService.correlacionar(...)` — un fallo de correlación (canal de
telemetría caído incluido) **nunca** revierte ni bloquea la creación del ticket, mismo principio
ya establecido para el *publish* de Kafka en ADR-0004.

Persistencia: tablas nuevas `incidencias` + `incidencia_tickets` en `ticket_db` (ver
`db-cluster/scripts/init_db.sql`), no particionadas — es una agrupación, no el registro de
negocio principal, mismo criterio que la tabla `technicians`.

## Escenario 4 del protocolo experimental (dos averías simultáneas)
Ni `c1` ni `c2` están diseñadas para resolver el caso de dos averías reales golpeando la misma
zona dentro de la misma ventana — ambas pueden fundirlas en una sola incidencia. Esto es
intencional: el propio texto de la guía dice que el punto del Escenario 4 es *revelar* que una
estrategia que agrupa bien una avería puede fundir erróneamente dos, no que la estrategia deba
evitarlo. El experimento (`experimentos/inyector_averias.py` +
`experimentos/analizar_correlacion.py`) existe para medir ese fenómeno con datos reales, no para
demostrar que no ocurre.

## Consecuencias

**Positivas:**
- El mecanismo es real y verificable de punta a punta: pruebas unitarias de las 3 estrategias
  (incluida la caída simulada del canal de telemetría) y una corrida manual real contra el stack
  completo levantado, antes de comprometerse a la batería estadística completa de 100 corridas.
- `GET /api/v1/tickets/incidencias` deja el agrupamiento consultable para el script de análisis,
  sin inventar un canal de exportación nuevo.
- Reutiliza exactamente el mismo esqueleto Repository/JPA-entity/mapper que ya usa `Ticket`, y el
  mismo criterio de "puerto en domain, adaptador en infrastructure" que `EventPublisher`/Kafka.

**Negativas / riesgos:**
- Es infraestructura construida retroactivamente para cumplir un requisito de la guía, no algo
  que haya evolucionado con el dominio desde entregas anteriores — igual que PE-U1, este ADR lo
  deja explícito en vez de simular una historia que no ocurrió.
- `c2` corrobora "algo está pasando en la zona", no distingue **cuál** avería — no resuelve el
  Escenario 4, según lo explicado arriba.
- La batería estadística completa (100 corridas, Mann-Whitney U, Â₁₂ de Vargha-Delaney,
  intervalos de confianza binomiales) todavía no se ha corrido — este ADR documenta el
  mecanismo, no sus resultados experimentales, que quedan para una fase de trabajo aparte.
