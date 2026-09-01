# ADR-0007: Canal de telemetría PE-U1 (sockets TCP + gRPC + reloj de Lamport)

## Estado
Aceptado — Entrega 4 (Agosto 2026)

## Contexto
La "Guía de Reutilización y Entrega — PFC Entrega 4" da por hecho, en tres tablas distintas
(inventario del equipo, estructura del repositorio y lo que debía estar terminado desde la
semana 4), que ACC ya tiene construido — desde una actividad "PE-U1" de la Entrega 1 — un
servidor y clientes de sockets TCP, un servicio gRPC y relojes de Lamport, que sirven de canal
para la telemetría de los equipos del abonado y los latidos de los nodos, y como base del orden
causal del ciclo de vida del ticket.

Se verificó contra el historial completo del repositorio (no solo el estado actual): no existe
ningún `.proto`, ningún código de sockets, ningún reloj de Lamport, en ningún commit. No es una
pérdida ni un artefacto no versionado — nunca se construyó. Esto se confirmó con el equipo antes
de decidir cómo proceder, con solo días hasta el cierre de la entrega; se optó por construirlo
ahora como una pieza real, en vez de tratarlo como una casilla de documentación a rellenar.

**Alcance de esta decisión.** Este ADR cubre la construcción del canal en sí (servidor, clientes
simuladores, reloj lógico, consulta vía gRPC), verificado y probado de forma aislada. No cubre
todavía integrarlo con la creación de tickets ni con la estrategia `CORREL` (modo `c2`) que lo
consumiría en producción — esa integración es una decisión aparte, pendiente de confirmar.

## Decisión
Se construye `services/telemetry-service/`, un microservicio Java 21 + Spring Boot más, con el
mismo *stack* y las mismas convenciones que `auth-service`/`ticket-service`/`report-service`
(mismo `pom.xml` base, mismo `Dockerfile` multietapa, mismas variables `OTEL_*`).

1. **Reloj lógico de Lamport** (`domain/LamportClock.java`), sin dependencias de framework:
   `tick()` para eventos locales, `update(recibido)` que aplica `max(local, recibido) + 1` al
   recibir un mensaje — la regla clásica de Lamport (1978) para establecer orden causal entre
   procesos con relojes físicos independientes.
2. **Servidor de sockets TCP** (`infrastructure/socket/TelemetrySocketServer.java`, puerto
   `9500`), que acepta líneas JSON de cualquier cliente y les asigna el timestamp de Lamport
   resultante antes de guardarlas en un buffer en memoria por zona (`TelemetryStore`, con
   ventana deslizante — mismo principio que los modos `c1`/`c2` de `CORREL`, que tampoco miran
   historial completo).
3. **Dos clientes simuladores** (`infrastructure/socket/simulator/`): uno para equipos del
   abonado (reportes de nivel de señal por zona) y otro para latidos de nodo (uno por cada nodo
   real del clúster CockroachDB: `roach1`/`roach2`/`roach3`). Cada cliente mantiene su propio
   reloj de Lamport, independiente del servidor, para que el timestamp final demuestre de verdad
   la regla `max(local, recibido) + 1` entre procesos distintos y no un número inventado.
4. **Servicio gRPC** (`infrastructure/grpc/`, puerto `9095`, contrato en
   `src/main/proto/telemetry.proto`): un único RPC, `GetEventosPorZona(zona, ventana)`, que
   devuelve los eventos de esa zona **ordenados por timestamp de Lamport** (orden causal), no
   por orden de llegada. Es el punto de consumo que usará la futura integración con `CORREL`.

Sin persistencia en CockroachDB ni paso por Kafka a propósito: es un canal de telemetría, no un
registro de negocio — el propio dominio del sistema (`Ticket`) no gana ningún campo nuevo por
esto.

## Numeración del ADR
El siguiente número disponible en `docs/adr/` es `0007`, no `0001`: los ADR `0001` y `0002` que
la guía asume como ya existentes nunca se crearon en ningún commit de este repositorio (mismo
hallazgo que el resto del canal). Reutilizar el número `0001` ahora sería engañoso — daría a
entender que este documento es el original de la Entrega 1, cuando en realidad se escribe en la
Entrega 4 para una pieza que se construyó en la Entrega 4.

## Consecuencias

**Positivas:**
- El canal es real y verificable de punta a punta: `mvn test` (8/8, incluida una prueba de
  concurrencia del reloj y una prueba de red real contra el servidor de sockets), y una prueba
  manual con los dos simuladores contra el servicio ya empaquetado en Docker.
- Consultarlo no requiere tocar ningún otro servicio: `GetEventosPorZona` queda disponible por
  gRPC de inmediato para cuando se decida cablear la Adición 1 de `CORREL`.
- Reutiliza exactamente las mismas convenciones que el resto de servicios Java del repo (mismo
  Dockerfile, mismas variables de entorno, mismo formato de ADR), así que no introduce un patrón
  nuevo que el equipo tenga que aprender aparte.

**Negativas / riesgos:**
- Es infraestructura construida retroactivamente para cumplir un requisito de la guía, no algo
  que haya evolucionado con el dominio desde la Entrega 1 — el propio texto de este ADR lo deja
  explícito, en vez de simular una historia que no ocurrió.
- El buffer de telemetría es en memoria: se pierde si el contenedor se reinicia. Aceptable para
  un canal de telemetría de ventana deslizante (no es el registro de tickets), pero es una
  limitación real si en el futuro se necesitara auditar telemetría histórica.
- Ningún servicio consume todavía este gRPC en producción — su valor real para el sistema queda
  pendiente de la decisión de integrarlo con `CORREL`, que es una pieza de trabajo aparte.
