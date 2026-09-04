# C4 Nivel 1 — Diagrama de contexto del sistema

Equipo ACC — Soporte Técnico ISP. Vista de más alto nivel: el sistema como una sola caja, sus
usuarios, y por qué no hay sistemas externos reales que integrar (todo lo que podría parecer
externo — notificaciones multicanal, telemetría de equipos del abonado — está simulado
internamente, documentado como tal en cada ADR correspondiente, no oculto detrás de una
integración real). Este archivo renderiza el diagrama directamente en GitHub (bloque Mermaid) —
para incluirlo en el documento LaTeX, exportar como PNG desde
[mermaid.live](https://mermaid.live) pegando el bloque de abajo.

```mermaid
C4Context
    title Sistema de Soporte Técnico ISP — equipo ACC (Nivel 1: Contexto)

    Person(cliente, "Cliente", "Abonado del ISP que reporta incidencias de conectividad")
    Person(tecnico, "Técnico de campo", "Resuelve tickets de su zona, cierra en sitio con evidencia")
    Person(admin, "Administrador", "Gestiona cuentas, ve reportes y el panel de observabilidad")

    System(sistema, "Sistema de Soporte Técnico ISP", "Gestión de tickets de soporte técnico, con aplicación web y móvil, sobre persistencia distribuida real")

    Rel(cliente, sistema, "Reporta y consulta sus tickets", "HTTPS, vía apps/web")
    Rel(tecnico, sistema, "Ve tickets asignados, cierra en sitio (foto + GPS)", "HTTPS, vía apps/mobile")
    Rel(admin, sistema, "Administra y ve reportes", "HTTPS, vía apps/web")

    UpdateElementStyle(sistema, $bgColor="#1F4E78", $fontColor="#ffffff")
```

## Por qué no hay sistemas externos en este nivel

- Las **notificaciones multicanal** (`notification-service`) están explícitamente simuladas —
  no hay integración real con un proveedor de SMS/email/push (ver el propio código:
  `dispatcher.js` genera el contenido y lo guarda en Mongo, no lo envía a ningún servicio
  externo).
- La **telemetría de equipos del abonado y latidos de nodo** (PE-U1, `telemetry-service`) es un
  canal simulado dentro del propio sistema (ver ADR-0007), no una integración con hardware real
  de campo.
- El **clúster CockroachDB**, Kafka y MongoDB son parte de la infraestructura propia del sistema
  (nivel 2), no sistemas externos de terceros.
