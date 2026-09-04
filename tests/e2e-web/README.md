# Pruebas E2E (web)

Esta carpeta la reserva el Listado 3 de la guía de entrega. Las pruebas E2E reales viven en
[`apps/web/e2e/`](../../apps/web/e2e), usando Playwright configurado en
[`apps/web/playwright.config.ts`](../../apps/web/playwright.config.ts) para correr en tres
motores de renderizado (Chromium, Firefox, WebKit). Se ejecutan como parte del *job*
`integration` de CI/CD (`.github/workflows/ci-cd.yml`), contra el backend real levantado con
`docker compose up`, no contra *mocks*.

Esta carpeta se deja como punto de entrada documentado, no como una copia física del código: una
copia se desactualizaría en cuanto el original cambie. Ver Sección "Pruebas y CI/CD" del
manuscrito (`docs/latex/secciones/pruebas_cicd.tex`) para el detalle completo, incluida la
depuración real de este *job* (Sección "Depuración real del job `integration`").
