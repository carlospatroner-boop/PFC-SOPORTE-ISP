# Declaración de uso de Inteligencia Artificial

**Proyecto:** Sistema de Gestión de Solicitudes de Soporte Técnico de Internet (equipo ACC)
**Asignatura:** Aplicaciones Distribuidas (ISR-701) — Entrega 4
**Universidad Técnica Estatal de Quevedo, Facultad de Ciencias de la Computación**

## Herramienta utilizada

**Claude Code** (Anthropic, modelo Claude Sonnet), usado como asistente de programación
conversacional dentro del propio editor/terminal del equipo, a lo largo de las Entregas 2, 3 y 4.

## Alcance del uso de IA

El asistente se usó para:

- **Scaffolding y boilerplate**: estructura inicial de los 5 microservicios (`auth-service`,
  `ticket-service`, `notification-service`, `ai-service`, `report-service`), sus `pom.xml`/
  `package.json`/`requirements.txt`, Dockerfiles, y configuración de Spring Boot/Express/FastAPI.
- **Implementación guiada**: lógica de negocio (autenticación JWT, RBAC, saga por coreografía vía
  Kafka, modelo de lectura CQRS, pipeline de Spark) escrita por el asistente bajo instrucciones y
  decisiones de diseño explícitas del equipo en cada paso.
- **Refactorización**: por ejemplo, el cambio de estrategia de fragmentación de `tickets` (de zona
  geográfica a `fecha_apertura`) para alinear el proyecto con la rúbrica oficial de la Entrega 3.
- **Depuración**: identificación y corrección de errores encontrados en pruebas reales contra el
  cluster CockroachDB, Kafka y los microservicios en ejecución (no solo revisión estática de
  código) — documentados con su causa raíz en los mensajes de commit y comentarios del código
  donde aporta contexto no obvio.
- **Pruebas**: generación de pruebas unitarias y de integración (JUnit/Mockito/AssertJ,
  Testcontainers, `node:test`, `pytest`), siempre ejecutadas y verificadas en vivo, no solo escritas.
- **Documentación**: redacción de READMEs, ADRs (Architectural Decision Records) y comentarios de
  código explicando decisiones de diseño no evidentes por sí solas.
- **Entrega 4 — específicamente**: refactor de `ticket-service` a arquitectura hexagonal de cuatro
  capas con seis patrones GoF (ADR-0005); construcción desde cero de `telemetry-service` (canal
  PE-U1: sockets TCP, reloj de Lamport, gRPC — ADR-0007) y del mecanismo de correlación de
  tickets en Incidencias (`CORREL`, estrategias `c0`/`c1`/`c2` — ADR-0008); depuración real del
  pipeline de CI/CD (tres causas raíz distintas encontradas y corregidas contra el *runner* real
  de GitHub Actions, no simuladas); instrumentación de observabilidad de las tres señales
  (métricas, *logs*, trazas); redacción del manuscrito LaTeX de esta entrega bajo dirección y
  revisión del equipo en cada sección.

## Verificación humana del contenido generado (Entrega 4)

Todo hallazgo, número y afirmación técnica de esta entrega que aparece en el manuscrito fue
contrastado por el equipo contra evidencia real antes de aceptarse — no se limitó a una revisión
de que el texto "sonara bien". Ejemplos concretos: los conteos de pruebas reportados en la
Sección de CI/CD se verificaron contando archivos de prueba reales, no se copiaron de una
ejecución anterior; los tres diagnósticos de la depuración de CI/CD se confirmaron cada uno contra
el *runner* real de GitHub Actions antes de documentarse como corregidos; los resultados de la
evaluación ISO/IEC 25010 provienen de corridas reales de Locust contra el sistema levantado, no de
estimaciones.

## Lo que NO se delegó a la IA

- Las **decisiones de arquitectura** (elección de CockroachDB, estrategia de fragmentación,
  patrón de saga por coreografía, límites entre microservicios) fueron dirigidas y aprobadas por
  el equipo en cada paso, no generadas de forma autónoma.
- La **verificación funcional**: todo cambio significativo se probó en vivo (creación de tickets
  reales, pruebas de fallos de nodos, inspección de métricas Prometheus, ejecución del pipeline
  Spark) antes de darse por válido — el asistente reportó explícitamente cuando algo no pudo
  verificarse en vivo, en vez de asumir que el código generado funcionaba.
- La **decisión final** de qué incluir, qué priorizar y qué queda fuera de alcance en cada entrega
  fue siempre del equipo.

## Honestidad sobre limitaciones del proyecto (referencia cruzada)

Varias partes del proyecto documentan explícitamente sus propias limitaciones en vez de
presentarse como más sofisticadas de lo que son, coherente con el espíritu de esta declaración:

- El dataset de telemetría de Spark es **sintético**, no telemetría real de un ISP (ver
  `spark/src/generate_dataset.py`).
- La clasificación de tickets en `ai-service` es un **clasificador basado en reglas/palabras
  clave**, no un modelo de NLP entrenado (ver `services/ai-service/README.md`).
- Las notificaciones de `notification-service` son **simuladas** (se guardan en MongoDB, no se
  envían de verdad por no contar con un proveedor de email/SMS real, ver
  `services/notification-service/README.md`).
- El canal de telemetría **PE-U1**, pese a su nombre, no existía en ninguna entrega anterior del
  repositorio: se verificó contra el historial completo de *commits* y se construyó por primera
  vez en la Entrega 4, junto con `CORREL` (ver la sección "Canal de telemetría PE-U1 y
  correlación de tickets en Incidencias" del manuscrito, y ADR-0007/ADR-0008).
- Las estrategias `c1`/`c2` de `CORREL` no están diseñadas para separar dos averías reales y
  simultáneas en la misma zona — pueden fundirlas en una sola incidencia. Es intencional: el
  objetivo del protocolo experimental es revelar ese error, no ocultarlo.
- La batería estadística completa del protocolo `CORREL` (10 repeticiones × 4 escenarios × 3
  modos) queda como trabajo de cierre pendiente; lo verificado en esta entrega es una corrida
  manual real de punta a punta que confirma el mecanismo funcionando, no la evaluación estadística
  completa.

## Integrantes del equipo

- Carlos José Carpio Mendoza
- Cristhian Daniel Pacheco Cárdenas
- Robinson Rodrigo Cando Moreno
- Jeremy Álvarez

Todos los integrantes revisaron y son responsables del código final entregado, independientemente
de qué partes contaron con asistencia de IA en su redacción inicial.
