package ec.edu.uteq.soporte.ticketservice.event;

/**
 * Payload publicado en el topico "ticket.created" cuando se crea un ticket.
 * Consumido por ai-service (clasifica categoria/prioridad), notification-service
 * (notifica al cliente) y report-service (siembra la fila del modelo de lectura
 * CQRS) -- ver TicketService.createTicket. Se serializa como JSON plano (sin
 * headers de tipo Java) para no acoplar el contrato a Java/Jackson; agregar
 * campos aqui es compatible hacia atras (los consumidores existentes los ignoran).
 */
public record TicketCreatedEvent(String ticketId, String zone, String clientId, String description, String createdAt) {
}
