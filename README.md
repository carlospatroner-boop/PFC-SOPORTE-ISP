# Sistema de Gestión de Solicitudes de Soporte Técnico de Internet (equipo ACC)

Proyecto Fin de Curso — Aplicaciones Distribuidas (ISR-701), Universidad Técnica Estatal de
Quevedo, Facultad de Ciencias de la Computación.

Sistema distribuido de gestión de tickets de soporte técnico para un ISP: dos aplicaciones
cliente (web y móvil) que consumen la misma API a través de un único API Gateway, seis
microservicios, persistencia distribuida real (CockroachDB), mensajería asíncrona (Kafka),
observabilidad completa (métricas + logs + trazas) y un pipeline de CI/CD de 7 jobs.

> ⚠️ **Importante — fecha límite de entrega indicada por el docente:** los commits para la
> actividad **GA-SUM-06 / PE-U5 - CI/CD, Pruebas y Observabilidad** se pueden realizar y subir
> al repositorio hasta el **viernes 28 de agosto de 2026**.

## Arquitectura

Todo el tráfico de los clientes pasa por un único punto de entrada:

```
apps/web (React) ──┐
                     ├──▶ api-gateway (8000) ──▶ microservicios ──▶ CockroachDB / Kafka / MongoDB
apps/mobile (Kotlin)┘
```

| Componente | Puerto | Stack | Responsabilidad |
|---|---|---|---|
| `apps/web` | 5173 | React 18 + TypeScript + Vite | Consola de operadores/clientes (SPA) |
| `apps/mobile` | — (APK) | Kotlin + Jetpack Compose | Cliente de campo para técnicos |
| `api-gateway` | 8000 | Spring Cloud Gateway | Único punto de entrada, enrutamiento por prefijo |
| `auth-service` | 8001 | Java 21 + Spring Boot | Autenticación JWT, RBAC (CLIENTE/TECNICO/ADMIN) |
| `ticket-service` (`svc-principal`) | 8002 | Java 21 + Spring Boot + CockroachDB | CRUD de tickets, arquitectura hexagonal, 6 patrones GoF |
| `notification-service` | 8003 | Node.js + Express + MongoDB | Notificaciones multicanal (simuladas) |
| `ai-service` | 8004 | Python + FastAPI + MongoDB | Clasificación asíncrona de tickets (basada en reglas) |
| `report-service` | 8005 | Java 21 + Spring Boot + CockroachDB | Modelo de lectura CQRS, reportes, exportación CSV |

Más: cluster CockroachDB de 3 nodos, Kafka + MongoDB para la saga por coreografía, y una pila de
observabilidad completa (OpenTelemetry Collector, Tempo, Prometheus, Grafana, cAdvisor).

Ver [`docs/diagrams/`](docs/diagrams/) para los diagramas C4 completos y
[`docs/adr/`](docs/adr/) para las decisiones de arquitectura documentadas.

## Cómo levantar todo desde cero

Todo el stack (17 contenedores) se levanta con un único `docker-compose.yml` en la raíz del
repositorio — sin pasos manuales de inicialización de base de datos, sin compose files sueltos
por carpeta:

```bash
docker compose up -d --build
```

Esto levanta, en orden de dependencias:

1. **Cluster CockroachDB** (`roach1`/`roach2`/`roach3`) + `db-init` (un contenedor de un solo uso
   que crea las bases de datos y ejecuta las semillas — determinista, seguro de correr más de una vez).
2. **Kafka + MongoDB** para la saga por coreografía entre `ticket-service`, `ai-service`,
   `notification-service` y `report-service`.
3. Los **6 microservicios** y `api-gateway`.
4. **`apps/web`** servida por nginx en `http://localhost:5173`.
5. **Observabilidad**: OpenTelemetry Collector, Tempo, Prometheus, cAdvisor y Grafana.

En Windows, `start-all.ps1` hace lo mismo con verificaciones adicionales (espera a que cada
servicio esté realmente saludable antes de continuar, corrige el reloj de la VM de Docker
Desktop si detecta desfase — ver `scripts/clock_watchdog.sh`).

```powershell
powershell -ExecutionPolicy Bypass -File start-all.ps1
```

### Cuentas de prueba (creadas automáticamente por `db-init`)

| Rol | Correo | Contraseña |
|---|---|---|
| ADMIN | `admin@soporte.local` | `Admin123!` |
| CLIENTE | `cliente@test.com` | `Passw0rd!` |

### Accesos una vez levantado

| Servicio | URL |
|---|---|
| Aplicación web | http://localhost:5173 |
| API (a través del gateway) | http://localhost:8000/api/v1/... |
| Grafana (dashboard operativo) | http://localhost:3000 (sin login, acceso anónimo de solo lectura) |
| Prometheus | http://localhost:9090 |
| Tempo (trazas, vía API) | http://localhost:3200 |

### Aplicación móvil

```bash
cd apps/mobile
./gradlew assembleDebug
# APK en apps/mobile/app/build/outputs/apk/debug/app-debug.apk
```

Requiere que `api-gateway` sea alcanzable desde el emulador/dispositivo — por defecto apunta a
`10.0.2.2:8000` (el alias que usa el emulador de Android para "localhost" de la máquina host).

## Variables de entorno

Ver [`.env.example`](.env.example). El `docker-compose.yml` no requiere ningún `.env` para
levantarse (todos los valores traen un default funcional para desarrollo local); el archivo
documenta qué se puede sobreescribir si se corre algún servicio suelto, fuera de Docker.

## Pruebas

| Capa | Comando | Dónde |
|---|---|---|
| Unitarias backend (Java) | `mvn test` | `services/auth-service`, `services/svc-principal`, `services/report-service` |
| Unitarias backend (Node) | `npm test` | `services/notification-service` |
| Unitarias backend (Python) | `pytest tests/ -v` | `services/ai-service` |
| Cobertura (JaCoCo) | `mvn test -Pcoverage` | `services/svc-principal` (reporte en `target/site/jacoco/`) |
| Componente + contrato Pact (web) | `npm test` | `apps/web` |
| Contrato Pact (proveedor) | `mvn test -Dtest=TicketServiceProviderPactTest -DRUN_CONTRACT_VERIFICATION=true` | `services/svc-principal` (requiere el stack completo levantado) |
| E2E (Chromium/Firefox/WebKit) | `npx playwright test` | `apps/web` |
| Unitarias móvil | `./gradlew testDebugUnitTest` | `apps/mobile` |
| Instrumentadas móvil (Compose Testing) | `./gradlew connectedDebugAndroidTest` | `apps/mobile` (requiere emulador/dispositivo) |
| Carga | `locust -f tests/load/locustfile.py --host http://localhost:8000` | raíz del repo |

Pipeline completo de CI/CD (7 jobs: lint, test-backend, test-web, test-mobile, build-images,
build-mobile-apk, integration): [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml).

## Documentación

- Decisiones de arquitectura: [`docs/adr/`](docs/adr/)
- Diagramas C4: [`docs/diagrams/`](docs/diagrams/)
- Manuscrito LaTeX (acumulado E1-E4, documento único `main.tex`): [`docs/latex/`](docs/latex/) —
  para compilarlo:
  ```bash
  cd docs/latex
  pdflatex -interaction=nonstopmode main.tex
  bibtex main
  pdflatex -interaction=nonstopmode main.tex
  pdflatex -interaction=nonstopmode main.tex   # dos pasadas más para resolver referencias/bibliografía
  ```
  Requiere una distribución TeX Live completa (paquetes `babel`, `booktabs`, `hyperref`,
  `enumitem`, entre otros). Si no se tiene TeX Live instalado localmente, se puede usar la imagen
  Docker `texlive/texlive:latest-medium` montando `docs/` (no solo `docs/latex/`, porque las
  figuras del manuscrito referencian `../diagrams/*.png` con ruta relativa) y ejecutando los
  mismos cuatro comandos dentro del contenedor.
- Esquema de base de datos consolidado (referencia de lectura; los scripts que realmente se
  ejecutan siguen en `db-cluster/scripts/`): [`docs/db/schema.sql`](docs/db/schema.sql)
- Puntos de entrada documentados a las pruebas de integración, E2E y contrato (el código real
  vive junto a cada módulo, no se duplica aquí): [`tests/`](tests/)
- Protocolo y resultados experimentales (Spark, Entrega 3): [`docs/experimentos/protocolo.md`](docs/experimentos/protocolo.md)
- Evaluación experimental ISO/IEC 25010 (Entrega 4): [`docs/experimentos/evaluacion_iso25010.md`](docs/experimentos/evaluacion_iso25010.md)
- Evidencia de tolerancia a fallos: `docs/evidencias/`
- Declaración de uso de IA: [`ai-usage-declaration.md`](ai-usage-declaration.md)
- Actas de reunión (reconstruidas a partir del chat real de coordinación del equipo, con fecha,
  asistentes y decisiones reales): [`docs/actas_reunion.md`](docs/actas_reunion.md)

## Novedades de la Entrega 4

- **Refactor a arquitectura hexagonal** de `ticket-service` en 4 capas (presentación/aplicación/
  dominio/infraestructura) con 6 patrones GoF reales — ver [`docs/adr/0005-patrones-gof.md`](docs/adr/0005-patrones-gof.md).
- **Aplicación web** (`apps/web`, React + TypeScript) y **aplicación móvil** (`apps/mobile`,
  Kotlin + Jetpack Compose) — ver [`docs/adr/0006-eleccion-movil.md`](docs/adr/0006-eleccion-movil.md)
  para la justificación cuantitativa de Android nativo.
- **`api-gateway`**: único punto de entrada para ambos clientes.
- **Observabilidad completa**: métricas, logs JSON estructurados con `trace_id`, trazas
  distribuidas (OpenTelemetry), dashboard operativo en Grafana.
- **Pirámide de pruebas completa**: unitarias, integración (Testcontainers), contrato
  (Pact consumidor + proveedor), E2E (Playwright en 3 navegadores, Compose Testing en móvil),
  carga (Locust).
- **Pipeline de CI/CD de 7 jobs** con publicación de imágenes en GHCR.
- **Evaluación experimental contra ISO/IEC 25010**: 5 características medidas con datos reales
  (no simulados) — resultados honestos, incluyendo los umbrales que no se alcanzaron y por qué.
- **`telemetry-service` (canal PE-U1)**: servidor de sockets TCP + reloj de Lamport + gRPC para
  telemetría de equipos del abonado y latidos de nodo — ver
  [`docs/adr/0007-pe-u1-telemetria.md`](docs/adr/0007-pe-u1-telemetria.md).
- **Correlación de tickets en Incidencias (`CORREL`)**: tres estrategias intercambiables
  (`c0`/`c1`/`c2`, variable de entorno `CORREL`) para agrupar tickets de una misma avería, la
  última con evidencia real de telemetría vía gRPC — ver
  [`docs/adr/0008-correl-incidencias.md`](docs/adr/0008-correl-incidencias.md).

## Licencia

[MIT](LICENSE)
