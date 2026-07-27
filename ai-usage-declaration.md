# Declaración de uso de Inteligencia Artificial

**Proyecto:** Sistema de Gestión de Solicitudes de Soporte Técnico de Internet (equipo ACC)
**Asignatura:** Aplicaciones Distribuidas (ISR-701) — Entrega 3
**Universidad Técnica Estatal de Quevedo, Facultad de Ciencias de la Computación**

## Herramienta utilizada

**Claude Code** (Anthropic, modelo Claude Sonnet), usado como asistente de programación
conversacional dentro del propio editor/terminal del equipo, a lo largo de las Entregas 2 y 3.

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

## Integrantes del equipo

- Carlos José Carpio Mendoza
- Cristhian Daniel Pacheco Cárdenas
- Robinson Rodrigo Cando Moreno
- Jeremy Álvarez

Todos los integrantes revisaron y son responsables del código final entregado, independientemente
de qué partes contaron con asistencia de IA en su redacción inicial.
