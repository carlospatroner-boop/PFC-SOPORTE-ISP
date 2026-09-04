# Pruebas de contrato (Pact)

Esta carpeta la reserva el Listado 3 de la guía de entrega. Las pruebas de contrato reales usan
*consumer-driven contract testing* con Pact entre `apps/web` (consumidor) y `ticket-service`
(proveedor):

- **Lado consumidor**: pruebas dentro de `apps/web` que generan el contrato
  [`pacts/soporte-web-ticket-service.json`](../../pacts/soporte-web-ticket-service.json) a partir
  de las expectativas reales del cliente. Se ejecutan como parte del *job* `test-web`.
- **Lado proveedor**: verificación en
  [`services/svc-principal/src/test/java/.../contract/TicketServiceProviderPactTest.java`](../../services/svc-principal/src/test/java/ec/edu/uteq/soporte/ticketservice/contract/TicketServiceProviderPactTest.java),
  que reproduce cada interacción del contrato contra `ticket-service` real. Necesita el *stack*
  completo levantado (el servicio valida cada JWT llamando por HTTP a `auth-service`, no
  localmente), por lo que corre dentro del *job* `integration`, no de forma aislada.

Esta carpeta se deja como punto de entrada documentado, no como una copia física del código: una
copia se desactualizaría en cuanto el original cambie. Ver Sección "Pruebas y CI/CD" del
manuscrito (`docs/latex/secciones/pruebas_cicd.tex`, subsección "Pruebas de contrato") para el
detalle completo, incluidos los dos ajustes de diseño reales que se encontraron al verificar por
primera vez.
