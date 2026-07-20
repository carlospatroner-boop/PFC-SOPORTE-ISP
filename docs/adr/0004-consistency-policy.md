# ADR-0004: Posición CAP/PACELC y política de consistencia

## Estado
Aceptado — Entrega 3 (Junio 2026)

## Contexto
El teorema CAP [1] obliga a elegir, ante una partición de red, entre Consistencia (C) y
Disponibilidad (A). El modelo PACELC [2] extiende la discusión al caso sin partición: incluso con
la red sana, hay un trade-off entre Latencia (L) y Consistencia (C). El sistema del equipo ACC no
tiene un único punto en este espacio: distintas operaciones del dominio tienen requisitos
distintos, y forzar una sola postura global sería o bien insegura (para SLA/facturación) o
innecesariamente lenta (para la creación de tickets).

## Decisión
Se adopta una **postura CAP mixta por agregado de dominio**, coherente con lo discutido en la
Entrega 2 (Capítulo 5, clasificación REST/Kafka) y ahora extendida a la capa de datos:

- **Creación de tickets (agregado `Ticket`, operación de alta): AP.** Un abonado debe poder
  reportar una incidencia incluso si el cluster está parcialmente particionado o degradado. Se
  prioriza disponibilidad: la escritura se acepta localmente y se reconcilia después. CockroachDB,
  al usar Raft con quorum de mayoría, ya garantiza que mientras 2 de 3 nodos estén vivos y
  conectados entre sí, las escrituras se siguen sirviendo con consistencia serializable — la
  situación "AP" solo se manifiesta en el caso extremo de partición de red que aísle un nodo
  minoritario, el cual dejará de aceptar escrituras hasta reincorporarse (ver ADR sobre tolerancia
  a fallos y Anexo B, pregunta 2).
- **SLA y facturación (agregados `MetricaSLA`, datos económicos): CP.** Estas operaciones no
  toleran una lectura obsoleta ni una escritura duplicada: se exige consistencia serializable
  fuerte aun a costa de rechazar la operación si no hay quorum disponible. CockroachDB provee esto
  de forma nativa (no hay modo "eventual" configurable por tabla en este motor, a diferencia de
  bases de datos multi-modelo); la postura CP se refuerza a nivel de aplicación evitando
  reintentos automáticos silenciosos en el `report-service` cuando el cluster está degradado.
- **En el eje PACELC sin partición (E):** para el caso general (red sana), el sistema opta por
  **EC — prioriza Consistencia sobre Latencia** en las operaciones de escritura del `ticket-service`,
  porque CockroachDB replica de forma síncrona vía Raft antes de confirmar el commit. Se acepta el
  costo de latencia adicional (single-digit ms en LAN) a cambio de no introducir el trabajo de
  reconciliación que exigiría un modelo eventualmente consistente.

## Consecuencias

**Positivas:**
- La postura mixta refleja fielmente los requisitos reales del dominio (ya documentados en la E2)
  en lugar de forzar un único nivel de consistencia por conveniencia técnica.
- CockroachDB simplifica la implementación porque su consistencia serializable por defecto ya
  cubre el caso CP sin configuración adicional; el trabajo real está en la capa de aplicación
  (cómo se comporta el `ticket-service` ante errores 503/no-quorum).

**Negativas / riesgos:**
- Al no existir un modo "AP puro" nativo en CockroachDB, la disponibilidad ante partición severa
  (nodo minoritario aislado) se resuelve rechazando escrituras en ese nodo, no aceptándolas
  localmente con reconciliación posterior. Esto se documentará explícitamente como una limitación
  conocida frente al ideal teórico de la fila "AP" de la Tabla 1 de la guía, y se sustentará con
  la prueba experimental del Paso 4 (docker stop de un nodo minoritario vs. mayoritario).
- Requiere que el equipo pueda explicar, en la defensa oral, la diferencia entre la política
  *deseada* a nivel de dominio (AP para creación de tickets) y el comportamiento *real* que ofrece
  el motor de base de datos elegido (ver Anexo B, pregunta 2).

## Referencias
[1] E. Brewer, "CAP twelve years later: How the 'rules' have changed," *Computer*, vol. 45, no. 2,
    pp. 23–29, 2012.
[2] D. J. Abadi, "Consistency tradeoffs in modern distributed database system design: CAP is only
    part of the story," *Computer*, vol. 45, no. 2, pp. 37–42, 2012.
