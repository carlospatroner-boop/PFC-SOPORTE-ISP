package ec.edu.uteq.soporte.reportservice.domain.event;

/**
 * Copia local del payload de "ticket.classified", publicado por ai-service (ver
 * services/ai-service/app/kafka_consumer.py).
 */
public record TicketClassifiedEvent(String ticketId, String zone, String category, String priority) {
}
