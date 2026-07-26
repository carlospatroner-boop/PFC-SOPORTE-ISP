# C4 Nivel 2 — Diagrama de contenedores

Equipo ACC — Soporte Técnico ISP. Refleja el estado real del sistema tras la Entrega 3 (5
microservicios, cluster CockroachDB de 3 nodos particionado por `fecha_apertura`, Kafka + MongoDB
para la saga por coreografía). Este archivo renderiza el diagrama directamente en GitHub (bloque
Mermaid) — para incluirlo en el documento LaTeX, exportar como PNG desde
[mermaid.live](https://mermaid.live) pegando el bloque de abajo.

```mermaid
C4Container
    title Sistema de Soporte Técnico ISP — equipo ACC (Nivel 2: Contenedores)

    Person(cliente, "Cliente", "Abonado del ISP que reporta incidencias")
    Person(tecnico, "Técnico", "Resuelve tickets de su zona")
    Person(admin, "Administrador", "Gestiona cuentas y ve reportes")

    System_Boundary(sistema, "Sistema de Soporte Técnico ISP") {
        Container(frontend, "Frontend", "HTML/CSS/JS vanilla", "Dashboard de tickets + login")
        Container(auth, "auth-service", "Java 21 + Spring Boot", "JWT, RBAC, refresh tokens")
        Container(ticket, "ticket-service", "Java 21 + Spring Boot", "CRUD de tickets, orquesta la saga")
        Container(notif, "notification-service", "Node.js + Express", "Notificaciones multicanal (simuladas)")
        Container(ai, "ai-service", "Python + FastAPI", "Clasificación asíncrona por reglas")
        Container(report, "report-service", "Java 21 + Spring Boot", "Modelo de lectura CQRS, reportes")

        ContainerDb(crdb, "CockroachDB", "Cluster 3 nodos", "auth_db / ticket_db / report_db — PARTITION BY RANGE(fecha_apertura)")
        ContainerDb(mongo, "MongoDB", "Documento", "ai_db / notifications_db")
        ContainerQueue(kafka, "Kafka", "Message broker", "ticket.created / ticket.classified / ticket.status-changed / ticket.assigned")
    }

    Rel(cliente, frontend, "Usa", "HTTPS")
    Rel(tecnico, frontend, "Usa", "HTTPS")
    Rel(admin, frontend, "Usa", "HTTPS")

    Rel(frontend, auth, "Login/validate", "REST/JSON")
    Rel(frontend, ticket, "CRUD tickets", "REST/JSON + JWT")
    Rel(frontend, report, "Reportes (solo ADMIN)", "REST/JSON + JWT")

    Rel(ticket, auth, "Valida token", "REST/JSON")
    Rel(report, auth, "Valida token (solo ADMIN)", "REST/JSON")

    Rel(auth, crdb, "Lee/escribe", "JDBC")
    Rel(ticket, crdb, "Lee/escribe", "JDBC")
    Rel(report, crdb, "Lee/escribe", "JDBC")

    Rel(ticket, kafka, "Publica ticket.created/status-changed/assigned", "JSON")
    Rel(ai, kafka, "Consume ticket.created, publica ticket.classified", "JSON")
    Rel(ticket, kafka, "Consume ticket.classified", "JSON")
    Rel(notif, kafka, "Consume los 4 tópicos", "JSON")
    Rel(report, kafka, "Consume los 4 tópicos (modelo CQRS)", "JSON")

    Rel(ai, mongo, "Guarda clasificaciones", "ai_db")
    Rel(notif, mongo, "Guarda notificaciones simuladas", "notifications_db")
```
