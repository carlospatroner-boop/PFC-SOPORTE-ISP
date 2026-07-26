package ec.edu.uteq.soporte.ticketservice.event;

/**
 * Payload publicado en "ticket.status-changed" cuando cambia el estado de un
 * ticket. Consumido por notification-service (avisa al cliente) y report-service
 * (actualiza el modelo de lectura CQRS).
 */
public record TicketStatusChangedEvent(String ticketId, String zone, String oldStatus, String newStatus) {
}
