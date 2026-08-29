# Sistema de Gestión de Solicitudes de Soporte Técnico de Internet (equipo ACC)

Proyecto Fin de Curso — Aplicaciones Distribuidas (ISR-701), Universidad Técnica Estatal de
Quevedo, Facultad de Ciencias de la Computación.

Sistema distribuido de gestión de tickets de soporte técnico para un ISP, construido como una
arquitectura de microservicios con persistencia distribuida real (CockroachDB), mensajería
asíncrona (Kafka) y un pipeline analítico paralelo (Apache Spark).

> ⚠️ **Importante — fecha límite de entrega indicada por el docente:** los commits para la
> actividad **GA-SUM-06 / PE-U5 - CI/CD, Pruebas y Observabilidad** se pueden realizar y subir
> al repositorio hasta el **viernes 28 de agosto de 2026**.

## Arquitectura

| Microservicio | Puerto | Stack | Responsabilidad |
|---|---|---|---|
| `auth-service` | 8001 | Java 21 + Spring Boot | Autenticación JWT, RBAC (CLIENTE/TECNICO/ADMIN) |
| `ticket-service` (`svc-principal`) | 8002 | Java 21 + Spring Boot + CockroachDB | CRUD de tickets, orquesta la saga por Kafka |
| `notification-service` | 8003 | Node.js + Express + MongoDB | Notificaciones multicanal (simuladas) |
| `ai-service` | 8004 | Python + FastAPI + MongoDB | Clasificación asíncrona de tickets (basada en reglas) |
| `report-service` | 8005 | Java 21 + Spring Boot + CockroachDB | Modelo de lectura CQRS, reportes, exportación CSV |

Más: cluster CockroachDB de 3 nodos (`db-cluster/`), Kafka + MongoDB (`messaging/`), pipeline de
Spark (`spark/`) y un frontend estático (`frontend/`).

## Cómo levantar todo (Windows)

```powershell
powershell -ExecutionPolicy Bypass -File start-all.ps1
```

Levanta el cluster CockroachDB, Kafka/MongoDB, y los 5 microservicios, y deja el frontend servido
en `http://localhost:5500/auth/index.html`. Es seguro volver a correrlo si algo quedó a medias
(los pasos de base de datos usan `IF NOT EXISTS`).

Cuentas de prueba creadas automáticamente:

| Rol | Correo | Contraseña |
|---|---|---|
| ADMIN | `admin@soporte.local` | `Admin123!` |
| CLIENTE | `cliente@test.com` | `Passw0rd!` |

Para levantar cada pieza a mano (otros sistemas operativos, o depuración), ver el README de cada
carpeta: [`db-cluster/README.md`](db-cluster/README.md), [`services/auth-service/README.md`](services/auth-service/README.md),
[`services/svc-principal/README.md`](services/svc-principal/README.md),
[`services/notification-service/README.md`](services/notification-service/README.md),
[`services/ai-service/README.md`](services/ai-service/README.md),
[`services/report-service/README.md`](services/report-service/README.md),
[`spark/README.md`](spark/README.md), [`frontend/README.md`](frontend/README.md).

## Variables de entorno

Ver [`.env.example`](.env.example) — cada servicio ya trae un valor por defecto para correr en
localhost sin configurar nada.

## Novedades de la Entrega 3

- **Fragmentación horizontal de `tickets` por `fecha_apertura`** (`PARTITION BY RANGE`, 4
  particiones trimestrales) con replicación factor 3 — ver [`docs/adr/0003-sharding-policy.md`](docs/adr/0003-sharding-policy.md).
- **Pruebas de integración con Testcontainers** contra un CockroachDB real, incluida una prueba
  empírica de aislamiento serializable — `services/svc-principal/src/test/.../integration/`.
- **Métricas Prometheus incrementales** (`crdb_query_duration_seconds`,
  `crdb_transaction_retries_total`, `crdb_pool_active_connections`) en `/actuator/prometheus` de
  `ticket-service`.
- **Pipeline Apache Spark** (`spark/`): 5 transformaciones (filtrado, join colocalizado por
  `client_id`, ventana de reincidencia, tipos temporales, clustering ML por texto y zona) sobre un
  dataset sintético de ≥500,000 filas, con baseline secuencial en pandas para comparación
  (`spark/src/pipeline.py`, `spark/src/baseline.py`).
- **Protocolo experimental** (r=10 repeticiones, IC 95%, contraste estadístico) — ver
  [`docs/experimentos/protocolo.md`](docs/experimentos/protocolo.md).
- **Verificación de tolerancia a fallos** del cluster CockroachDB — ver
  [`db-cluster/README.md`](db-cluster/README.md) y `docs/evidencias/`.

## Tests

Cada microservicio Java tiene sus propios tests unitarios y de integración (`mvn test`); los
servicios de Node.js y Python usan `node --test` / `pytest` respectivamente. Ver el README de
cada servicio para el comando exacto.

## Documentación

- Decisiones de arquitectura: [`docs/adr/`](docs/adr/)
- Diagramas: [`docs/diagrams/`](docs/diagrams/)
- Documento LaTeX de la Entrega 3: [`docs/latex/`](docs/latex/)
- Protocolo experimental y resultados: [`docs/experimentos/`](docs/experimentos/)
- Evidencia de tolerancia a fallos: `docs/evidencias/`
- Declaración de uso de IA: [`ai-usage-declaration.md`](ai-usage-declaration.md)

## Diapositivas de la defensa (Entrega 3)

Mazo de la defensa oral en [`docs/diapositivas/`](docs/diapositivas/):
`EXPO-E3_ACC_Carpio-Pacheco-Cando-Alvarez.pptx` (fuente editable) y su exportación
`EXPO-E3_ACC_Carpio-Pacheco-Cando-Alvarez.pdf` (entregable, ambos deben coincidir).

El `.pptx` se genera con un script Python (`python-pptx`), no se edita a mano, para poder
regenerarlo de forma determinista si cambia algún dato (por ejemplo, tras ejecutar el protocolo
experimental o el video de tolerancia a fallos pendientes). Para reproducirlo:

```bash
pip install python-pptx matplotlib pillow
python docs/diapositivas/build_deck.py
```

Esto regenera `docs/diapositivas/EXPO-E3_ACC_Carpio-Pacheco-Cando-Alvarez.pptx` a partir de las
imágenes ya versionadas en `docs/diagrams/` y `spark/results/`. Para exportar el PDF a partir del
`.pptx` (usado aquí para no depender de una instalación local de LibreOffice):

```bash
docker run --rm -v "${PWD}/docs/diapositivas:/work" -w /work debian:bookworm-slim sh -c \
  "apt-get update -qq && apt-get install -y --no-install-recommends libreoffice-impress && \
   soffice --headless --convert-to pdf EXPO-E3_ACC_Carpio-Pacheco-Cando-Alvarez.pptx"
```

## Licencia

[MIT](LICENSE)
