# Pruebas de integración

Esta carpeta la reserva el Listado 3 de la guía de entrega. Las pruebas de integración reales
del sistema **no viven aquí** — viven dentro de cada módulo, junto al código que prueban, y desde
ahí ya se ejecutan como parte del *job* `test-backend` de CI/CD (`.github/workflows/ci-cd.yml`):

- [`services/svc-principal/src/test/java/.../TicketRepositoryIntegrationTest.java`](../../services/svc-principal/src/test/java) —
  contra un CockroachDB real levantado con Testcontainers, no un *mock*.
- [`services/svc-principal/src/test/java/.../TelemetryGrpcClientAdapterTest.java`](../../services/svc-principal/src/test/java) —
  contra un servidor gRPC real de `telemetry-service`.
- [`services/telemetry-service/src/test/java/.../TelemetrySocketServerIntegrationTest.java`](../../services/telemetry-service/src/test/java) —
  contra un servidor de sockets real, con un cliente `Socket` real.

Esta carpeta se deja como punto de entrada documentado, no como una copia física del código: una
copia se desactualizaría en cuanto el original cambie. Ver Sección "Pruebas y CI/CD" del
manuscrito (`docs/latex/secciones/pruebas_cicd.tex`) para el detalle completo.
