package ec.edu.uteq.soporte.ticketservice.event;

/**
 * Payload consumido desde el topico "ticket.classified", publicado por ai-service
 * tras clasificar un ticket. "category"/"priority" llegan como texto y se parsean
 * a los enums Category/Priority en TicketClassificationListener (con manejo de
 * valores desconocidos, por si ai-service y ticket-service llegan a desincronizar
 * su vocabulario de categorias).
 */
public record TicketClassifiedEvent(String ticketId, String zone, String category, String priority) {
}
