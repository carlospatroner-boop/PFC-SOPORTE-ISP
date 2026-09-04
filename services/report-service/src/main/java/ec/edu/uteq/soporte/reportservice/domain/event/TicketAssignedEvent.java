package ec.edu.uteq.soporte.reportservice.domain.event;

/**
 * Copia local del payload de "ticket.assigned" publicado por ticket-service (ver
 * services/svc-principal/.../domain/event/TicketAssignedEvent.java).
 */
public record TicketAssignedEvent(String ticketId, String zone, String technicianId) {
}
