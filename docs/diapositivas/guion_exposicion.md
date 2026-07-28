# Guion de la exposición — Entrega 3 (equipo ACC)

**Duración total: ~20 minutos · 18 diapositivas · ~1 min/diapositiva en promedio**
(la diapositiva 13, demo, puede tomar 2-3 min si se hace en vivo)

Este guion es para que cada quien prepare lo que va a decir, **no para leer en pantalla**. La
diapositiva muestra solo el ancla (título + 3-5 palabras clave); lo que sigue es lo que se dice en
voz alta al llegar a esa diapositiva. Practiquen su parte en voz alta al menos una vez antes de la
defensa — el tiempo se pasa volando.

---

## Diapositiva 1 — Portada
**(nadie habla, se proyecta mientras el primero en hablar se presenta brevemente)**

> "Buenas tardes, somos el equipo ACC. Hoy les presentamos la Entrega 3 del Sistema de Soporte
> Técnico ISP: la capa de datos distribuida y el pipeline analítico paralelo. Vamos a repartirnos
> la exposición: yo soy Carlos y abro con el objetivo de hoy."

---

## Diapositiva 2 — Objetivo de hoy y hoja de ruta
**Expone: Carlos Carpio**

> "El objetivo de esta entrega fue tomar la arquitectura de microservicios que construimos en la
> Entrega 2 y completarla con dos piezas que le faltaban a un sistema verdaderamente distribuido:
> una capa de datos real, fragmentada y replicada, y un pipeline de procesamiento paralelo que
> demuestre speedup medible.
>
> Vamos a recorrer cinco cosas: primero la arquitectura y qué cambió; después las decisiones
> técnicas detrás del diseño; luego la evidencia real de que todo esto funciona —no solo que lo
> escribimos, sino que lo probamos—; en el minuto 12 aproximadamente vamos a hacer una demo en
> vivo creando un ticket real; y cerramos con los riesgos que quedan y hacia dónde va esto en la
> Entrega 4."

---

## Diapositiva 3 — El problema: soporte técnico de un ISP
**Expone: Cristhian Pacheco**

> "Para quien no vio nuestras entregas anteriores, un recordatorio rápido del dominio: somos un
> sistema de soporte técnico para un proveedor de Internet. Tres tipos de usuario: el cliente que
> reporta una falla, el técnico que la resuelve en su zona, y el administrador que supervisa todo.
>
> Tres requisitos no funcionales nos han perseguido desde la Entrega 1 y son los que esta entrega
> pone a prueba de verdad: cumplir el SLA de resolución según la prioridad del ticket, mantener
> consistencia cuando dos personas actualizan el mismo ticket al mismo tiempo —piensen en un
> técnico cerrándolo mientras un administrador lo reasigna—, y poder escalar a un histórico de más
> de 500 mil incidencias sin que el sistema se arrodille."

---

## Diapositiva 4 — Arquitectura de contenedores (C4 Nivel 2)
**Expone: Jeremy Álvarez**

> "Este es el diagrama de contenedores actualizado. Los cinco microservicios de la Entrega 2 siguen
> igual, sin cambios de contrato: auth-service, ticket-service, notification-service, ai-service y
> report-service. Lo que cambia está marcado en color.
>
> En ámbar, CockroachDB: pasó de un esquema pensado para un solo nodo a un clúster real de tres
> nodos con la tabla de tickets fragmentada. En verde azulado, algo completamente nuevo: el
> pipeline de Spark, que se conecta a CockroachDB por JDBC y hace el análisis paralelo que van a
> ver más adelante."

---

## Diapositiva 5 — Qué cambió respecto a E2
**Expone: Jeremy Álvarez**

> "Puntualizando los cinco cambios concretos de esta entrega: la tabla tickets ahora se fragmenta
> por fecha de apertura, no por zona geográfica como en la Entrega 2 —en un momento Cristhian
> explica por qué—. El clúster de CockroachDB es real, tres nodos con replicación, no una base
> mono-nodo. Agregamos tres métricas de Prometheus nuevas al ticket-service. Agregamos pruebas de
> integración con Testcontainers, que levantan un CockroachDB real solo para probar, no un mock.
> Y agregamos el pipeline de Spark como componente completamente nuevo."

---

## Diapositiva 6 — Lo planificado vs. lo construido
**Expone: Robinson Cando**

> "Siendo honestos con el estado real: tres cosas están completamente hechas y verificadas —la
> fragmentación con su ADR, las pruebas de integración con las tres métricas de Prometheus sin
> ninguna advertencia, y el pipeline de Spark corriendo sobre seiscientas mil filas reales.
>
> Dos cosas siguen pendientes y lo decimos sin rodeos: el video de tolerancia a fallos con uno y
> dos nodos caídos, y el protocolo estadístico completo de cincuenta corridas con intervalo de
> confianza del 95%. Ambos están diseñados e implementados, solo falta ejecutarlos —lo explicamos
> más en la diapositiva de riesgos."

---

## Diapositiva 7 — Decisión: fragmentación por fecha_apertura
**Expone: Cristhian Pacheco**

> "Aquí está el fragmento real del esquema. La tabla tickets tiene una clave primaria compuesta,
> fecha de creación más id, y se particiona por rango de esa fecha en cuatro particiones
> trimestrales para este año.
>
> La alternativa que descartamos fue particionar por zona geográfica, que de hecho era nuestro
> diseño original en la Entrega 2. El criterio de la decisión no fue una preferencia nuestra: la
> guía oficial de la Entrega 3 exige explícitamente esta estrategia para nuestro equipo. Lo
> documentamos en un ADR, con la referencia bibliográfica de Özsu y Valduriez que define las tres
> condiciones que toda fragmentación correcta debe cumplir: completitud, reconstrucción y
> disyunción. Las tres las verificamos formalmente, están en el documento LaTeX."

---

## Diapositiva 8 — Ubicación CAP / PACELC del sistema
**Expone: Cristhian Pacheco**

> "Con este cambio, ¿dónde queda nuestro sistema en el espacio CAP? CockroachDB es explícitamente
> un sistema CP: prioriza consistencia sobre disponibilidad cuando hay una partición de red. Esto
> no es teoría abstracta para nosotros, lo vimos en vivo: cuando forzamos dos transacciones
> concurrentes sobre el mismo ticket, el clúster no dejó pasar las dos silenciosamente, rechazó una
> con un error de conflicto real, código SQLSTATE 40001.
>
> Esto es posible gracias al algoritmo de consenso Raft: cada rango de datos tiene su propio grupo
> Raft, y con nuestro factor de replicación de tres, el quórum de escritura es de dos. Eso significa
> que toleramos la caída de un nodo sin perder disponibilidad de escritura, pero no la de dos al
> mismo tiempo."

---

## Diapositiva 9 — Artefactos verificables en el repositorio
**Expone: Carlos Carpio**

> "Todo lo que estamos diciendo está en el repositorio, no son afirmaciones sueltas. La carpeta
> db-cluster tiene los tres nodos, el esquema y el ADR. El servicio principal tiene el refactor
> completo más las pruebas con Testcontainers. La carpeta spark tiene el pipeline, el baseline en
> pandas y el protocolo experimental sobre seiscientas mil filas. Y en tests/integration están las
> dos pruebas que corren contra un CockroachDB real, no simulado."

---

## Diapositiva 9b — Evidencia en vivo: métricas Prometheus reales
**Expone: Robinson Cando**

> "Esto es una captura real, no un mockup: la salida cruda del endpoint de métricas del
> ticket-service ahora mismo. Ahí están las dos métricas activas —conexiones del pool y reintentos
> de transacción—, con su nombre, su tipo y su descripción bien declarados.
>
> Y esta última línea es clave: corrimos el validador oficial de Prometheus, promtool, sobre esta
> misma salida, y devolvió cero advertencias. No es que digamos que las métricas están bien hechas,
> lo comprobamos con la herramienta que usa la industria."

---

## Diapositiva 10 — Verificación de corrección (medida, no supuesta)
**Expone: Robinson Cando**

> "Resumiendo la verificación de corrección del clúster en tres puntos, todos medidos: el
> aislamiento serializable lo comprobamos forzando un conflicto real entre dos transacciones
> concurrentes, no lo asumimos. Las tres métricas de Prometheus pasan el validador sin
> advertencias. Y las dos pruebas de integración con Testcontainers pasan en verde contra una
> instancia real de CockroachDB, cada vez que corre la suite de pruebas."

---

## Diapositiva 11 — Pipeline Spark sobre 600.000 incidencias
**Expone: Cristhian Pacheco**

> "Este pipeline aplica las cinco transformaciones exigidas —filtrado, join, ventana, tipos
> temporales y una etapa de machine learning— sobre un dataset sintético de seiscientas mil
> incidencias, con semilla fija para que sea reproducible.
>
> A la izquierda ven el embudo: de seiscientas mil filas totales, quinientas cincuenta y siete mil
> setecientas uno quedan después del filtrado, y de esas, un sesenta y ocho punto tres por ciento
> —trescientas ochenta mil setecientas cincuenta y tres— se marcan como reincidentes dentro de
> treinta días. A la derecha, el resultado del clustering por texto y zona con K-means: cinco
> grupos, ninguno trivial, uno dominante y cuatro más específicos. Esto no es una ejecución de
> prueba, es la salida real del notebook, ejecutado de punta a punta."

---

## Diapositiva 12 — Demo: crear ticket → Kafka → notificación
**Expone: Carlos Carpio**

> "Ahora sí, vamos a hacerlo en vivo. Voy a crear un ticket real como si fuera un cliente
> autenticado. Cuando lo envíe, el ticket-service lo va a guardar en CockroachDB y va a publicar un
> evento en Kafka. El notification-service, que corre de forma completamente independiente, va a
> consumir ese evento sin que nadie se lo pida y va a generar una notificación. Si todo sale bien,
> en menos de dos segundos deberíamos poder consultar esa notificación ya guardada."
>
> *(aquí se ejecuta la demo real: POST a /api/v1/tickets, y luego GET a
> /api/v1/notifications?ticketId=... para mostrar la notificación generada)*

---

## Diapositiva 13 — Resultado observado (respaldo de contingencia)
**Expone: Carlos Carpio**

> "Por si algo falla en el aula —proyector, red, lo que sea—, esta es la captura real de la misma
> demo que acabamos de correr hace unos días, con el mismo flujo exacto: el POST devuelve 201 con
> el ticket creado, y la consulta a notification-service devuelve la notificación ya generada,
> canal email, con el mensaje automático. El id del ticket coincide entre ambas llamadas, así que
> es evidencia de que el flujo asíncrono completo funciona de extremo a extremo, no una simulación."

---

## Diapositiva 14 — Riesgos, deuda técnica y plan hacia E4
**Expone: Robinson Cando**

> "Siendo transparentes sobre lo que queda por resolver. El riesgo más concreto: como ahora
> fragmentamos por fecha y no por zona, una consulta filtrada solo por zona tiene que cruzar las
> cuatro particiones. Lo mitigamos con un índice secundario sobre zona, y está medido en el
> documento.
>
> Como deuda técnica, no tenemos todavía un API Gateway único al frente de los cinco
> microservicios; queda planificado para la Entrega 4. Y lo que ya mencionamos: el video de
> tolerancia a fallos y el protocolo estadístico completo siguen pendientes de ejecución.
>
> Lo bueno: el clúster, las tres métricas y el pipeline de Spark quedan listos para que la Entrega
> 4 los use directamente, sin tener que rehacer nada."

---

## Diapositiva 15 — Conclusiones del equipo
**Expone: Jeremy Álvarez**

> "Tres conclusiones que nos deja este trabajo, no un resumen de lo que ya dijimos. Primera: la
> fragmentación que exige la guía no siempre coincide con el patrón de acceso real del dominio, y
> aprendimos que documentar ese costo explícitamente vale más que forzar un diseño que finja que no
> existe el trade-off.
>
> Segunda: verificar empíricamente con Testcontainers y con promtool no fue un trámite de la
> rúbrica, nos hizo encontrar un problema real de memoria en el clúster antes de que llegara a
> producción.
>
> Tercera: diseñar el pipeline de Spark para un objetivo propio de nuestro dominio —reincidencia y
> agrupamiento de incidencias— dio resultados mucho más interpretables que aplicar cinco
> transformaciones genéricas sin relación con el negocio."

---

## Diapositiva 16 — Declaración de uso de IA generativa
**(quien se sienta más cómodo puede leerla completa, es breve y no necesita improvisar)**

> "Usamos Claude Code como asistente de programación a lo largo de esta entrega, para scaffolding,
> implementación guiada, depuración y generación de pruebas. Todo el contenido de este mazo fue
> revisado por el equipo. Las decisiones de arquitectura y toda la verificación funcional —correr
> las pruebas, revisar las métricas, ejecutar el pipeline— siempre las hicimos nosotros, no la IA."

---

## Diapositiva 17 — Referencias
**(se proyecta, no hace falta leerla en voz alta salvo que pregunten)**

> "Ahí quedan las referencias completas en formato IEEE de todo lo que citamos: Özsu y Valduriez
> para fragmentación, Brewer y Abadi para CAP y PACELC, Ongaro y Ousterhout para Raft, y Zaharia
> para el modelo RDD de Spark. Quedamos atentos a sus preguntas."

---

## Notas rápidas para todos

- **No lean las viñetas de la pantalla textualmente** — la audiencia ya las está leyendo; digan lo
  mismo con sus propias palabras y un poco más de contexto, como está arriba.
- **Practiquen la transición entre personas** — decir algo como "le paso a Cristhian para las
  decisiones técnicas" ayuda a que no se sienta cortado.
- Si algo de la demo falla, **no se pongan nerviosos**: la diapositiva 13 es exactamente para eso,
  hay que decirlo con tranquilidad ("por si acaso, aquí está la misma corrida capturada hace unos
  días") y seguir.
- Cualquiera del equipo debe poder responder las 5 preguntas obligatorias del Anexo B de la guía
  (Raft y quórum, justificación de la clave de partición, ley de Amdahl y el 90% del speedup,
  amenazas a la validez interna, qué se reutiliza en E4) — repásenlas todos, no solo quien expuso
  esa parte.
