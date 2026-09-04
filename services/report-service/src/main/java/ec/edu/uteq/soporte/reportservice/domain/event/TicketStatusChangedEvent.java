package ec.edu.uteq.soporte.reportservice.domain.event;

/**
 * Copia local del payload de "ticket.status-changed" publicado por ticket-service
 * (ver services/svc-principal/.../domain/event/TicketStatusChangedEvent.java).
 */
public record TicketStatusChangedEvent(String ticketId, String zone, String oldStatus, String newStatus) {
}
