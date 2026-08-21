package ec.edu.uteq.soporte.ticketservice.domain.policy;

import ec.edu.uteq.soporte.ticketservice.domain.Priority;

import java.time.Duration;

/**
 * Patron Strategy: calcula el plazo de SLA aplicable. Antes de este refactor, el plazo por
 * defecto (24h) vivia hardcodeado en TicketService.createTicket() y el plazo por prioridad
 * (4h/12h/24h/48h) vivia hardcodeado por separado en TicketClassificationListener -- dos
 * lugares con la misma responsabilidad, sin nombre ni posibilidad de intercambiarse.
 *
 * Ahora ambas politicas implementan esta misma interfaz y son intercambiables: la creacion de
 * un ticket usa {@link DefaultSlaPolicy} (todavia no hay prioridad real); cuando ai-service
 * clasifica el ticket, se recalcula con {@link ClassifiedSlaPolicy}. Agregar una tercera
 * politica (por ejemplo, SLA distinto por zona) no requiere tocar el codigo que ya las usa.
 */
public interface SlaPolicy {
    Duration slaFor(Priority priority);
}
