# Plan de 2 semanas — Entrega 3 (TA-PFC-E3)

**Equipo:** ACC — Soporte Técnico ISP (Sistema de Gestión de Solicitudes para Soporte Técnico de Internet)
**Entrega:** Semana 13, viernes 23:59
**Estado de partida:** Solo existe el documento de diseño de la Entrega 2. No hay código, cluster ni pipeline implementados todavía.

## Por qué este orden

D1 (Diseño del esquema, 18%), D2 (Verificación experimental, 18%) y D3 (Pipeline paralelo, 18%) suman 54% de la nota y son los únicos criterios que exigen algo *corriendo de verdad* el día de la defensa. Si el cluster no levanta o el pipeline no corre, esos puntos se pierden sin posibilidad de diferimiento. Por eso van primero. El documento LaTeX (D4, 14%) se puede escribir en paralelo a medida que cada pieza queda lista — de hecho conviene que Documentación escriba la teoría (sección 3 del instructivo) desde el día 1, porque no depende de que el código funcione.

## Roles (reutilizando los de la Entrega 2)

| Rol | Persona | Frente principal en la E3 |
|---|---|---|
| Arquitecto | Cristhian Pacheco | Cluster CockroachDB, particionamiento, ADRs, diagrama C4 actualizado |
| Líder de Desarrollo | Carlos Carpio | Integración JDBC del microservicio con el cluster, pipeline Spark, análisis de speedup |
| Responsable de Calidad | Jeremy Álvarez | Prueba de tolerancia a fallos, benchmarking comparativo, tests de integración, checklist final |
| Responsable de Documentación | Robinson Cando | Documento LaTeX completo, ADRs (redacción), video de evidencia, declaración de uso de IA, bibliografía |

Todos deben poder responder las 5 preguntas obligatorias del Anexo B el día de la defensa — no basta con que cada uno domine solo su parte.

## Semana 1 — Infraestructura y datos (lo que puede romperse)

**Días 1–2 (arranque)**
- Levantar el repositorio con la estructura exacta del Listing 2 (carpetas `db-cluster/`, `spark/`, `docs/adr/`, `docs/evidencias/`).
- Levantar el cluster CockroachDB de 3 nodos localmente (`docker compose -f db-cluster/docker-compose.cockroach.yml up`) y verificar los 3 nodos `live` en la consola web (puerto 8080).
- Aplicar el esquema con `PARTITION BY LIST` sobre `tickets` (zona geográfica) — ver `db-cluster/scripts/init_db.sql`.
- Redactar ADR-0003 (fragmentación) y ADR-0004 (consistencia) — plantillas ya incluidas en este kit.

**Días 3–4 (dataset y benchmarking)**
- Generar el dataset sintético de telemetría/incidencias (≥500,000 filas) con `spark/src/generate_dataset.py`.
- Cargar datos de prueba en el cluster respetando la partición (`seed_partitioned.sql`, distribución ~40/30/30 entre zonas).
- Ejecutar las 5 queries de `queries_bench.sql` con `EXPLAIN ANALYZE` en el cluster de 3 nodos y en un nodo único (`cockroach start-single-node --insecure`). Llenar la tabla comparativa.

**Días 5–7 (tolerancia a fallos + arranque de Spark)**
- Ejecutar el escenario de fallo: `load_write.py` corriendo + `docker stop roach2` a mitad de la carga. Grabar el video simultáneo (consola web + terminal de carga + comando de stop).
- Medir P50/P95 antes y después, tiempo de recuperación, rangos subreplicados.
- Instalar PySpark localmente y correr `spark/src/pipeline.py` una vez en `local[1]` para validar que las 5 transformaciones (3+ con shuffle) ejecutan sin errores.
- Documentación: empezar a redactar la sección 3 (fundamento teórico, 4 subsecciones ≥300 palabras cada una) — no depende de que nada más esté listo.

## Semana 2 — Paralelismo, integración y cierre

**Días 8–9 (speedup y Amdahl)**
- Correr `amdahl_analysis.py` con `local[1,2,4,8]`, registrar tiempos, ajustar la fracción paralelizable *p* y generar las dos figuras (speedup vs. particiones, tiempo por transformación) a 300 dpi.

**Días 10–11 (integración real con el PFC)**
- Conectar el microservicio principal (`ticket-service`, Java 21 + Spring Boot) al cluster vía JDBC como backing store real de al menos una funcionalidad (por ejemplo, creación y consulta de tickets).
- Materializar el resultado del pipeline Spark como tabla en CockroachDB (o Parquet consumido por `report-service`).
- Actualizar el diagrama C4 nivel 2 con estos dos componentes integrados.

**Días 12–13 (documento y repositorio)**
- Terminar el documento LaTeX: diseño del esquema, implementación (listados), verificación experimental, análisis comparativo, pipeline Spark, discusión crítica, conclusiones individuales (≥150 palabras c/u), bibliografía IEEE (≥10 referencias con DOI/ISBN).
- Redactar `ai-usage-declaration.md` (obligatoria — su ausencia penaliza -0.3 sobre la nota final).
- Verificar ≥5 commits significativos por integrante, ausencia de credenciales (`gitleaks`), README raíz con instrucciones ejecutables paso a paso.
- Recorrer el checklist del Anexo A completo.

**Día 14 (buffer y ensayo)**
- Ensayar la defensa oral (12 min exposición + 5 min preguntas). Repasar las 5 preguntas obligatorias del Anexo B con todo el equipo.
- Ensayo de la demo en vivo: cluster arriba, prueba de fallos reproducida, notebook Spark corrido — sin depender de diapositivas.

## Riesgos a vigilar desde ya

- **Dataset real inexistente**: como es un proyecto académico, lo más realista es generar telemetría/incidencias sintéticas coherentes con el dominio (ya incluido en este kit) y documentarlo honestamente como tal en el LaTeX — la rúbrica acepta datos sintéticos si están bien documentados.
- **Microservicio Java real**: la guía exige Java 21 + Spring Boot para el servicio central. Si el equipo no llegó a programarlo en la E2, hay que priorizar un `ticket-service` mínimo pero funcional (CRUD + conexión JDBC) antes que features extra.
- **Tiempo de Spark**: instalar y correr Spark localmente puede tener fricción de entorno (Java version, PySpark). Probarlo el día 3, no el día 12.
