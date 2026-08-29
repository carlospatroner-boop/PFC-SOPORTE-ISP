package ec.edu.uteq.soporte.ticketservice.domain.event;

/**
 * Payload publicado en "ticket.escalated" cuando la cadena de escalado (Chain of
 * Responsibility, ver domain/escalation/) decide escalar un ticket. A diferencia de
 * "ticket.status-changed" (que ya existia), este evento es nuevo: antes de la Entrega 4
 * ningun ticket llegaba nunca al estado ESCALADO, asi que este evento nunca se disparaba.
 */
public record TicketEscalatedEvent(String ticketId, String zone, String motivo) {
}
