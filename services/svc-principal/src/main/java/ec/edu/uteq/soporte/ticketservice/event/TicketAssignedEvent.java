package ec.edu.uteq.soporte.ticketservice.event;

/**
 * Payload publicado en "ticket.assigned" cuando se asigna un tecnico a un ticket.
 * Consumido por notification-service (avisa al cliente, tipicamente por 2 canales
 * por ser mas urgente) y report-service (actualiza el modelo de lectura CQRS).
 */
public record TicketAssignedEvent(String ticketId, String zone, String technicianId) {
}
