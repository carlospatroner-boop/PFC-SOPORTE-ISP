package ec.edu.uteq.soporte.ticketservice.event;

/**
 * Payload publicado en el topico "ticket.created" cuando se crea un ticket.
 * Consumido por ai-service (Python) para clasificar categoria/prioridad -- ver
 * TicketService.createTicket y el README de ai-service. Se serializa como JSON
 * plano (sin headers de tipo Java) para no acoplar el contrato a Java/Jackson.
 */
public record TicketCreatedEvent(String ticketId, String zone, String description) {
}
