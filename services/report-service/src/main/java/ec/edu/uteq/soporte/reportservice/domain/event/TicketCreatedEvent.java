package ec.edu.uteq.soporte.reportservice.domain.event;

/**
 * Copia local del payload de "ticket.created" publicado por ticket-service (ver
 * services/svc-principal/.../domain/event/TicketCreatedEvent.java) -- cada servicio
 * mantiene su propia copia del contrato, no hay modulo compartido en este repo.
 */
public record TicketCreatedEvent(String ticketId, String zone, String clientId, String description, String createdAt) {
}
