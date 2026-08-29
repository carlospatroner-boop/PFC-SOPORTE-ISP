package ec.edu.uteq.soporte.ticketservice.domain.escalation;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;

import java.util.Optional;

/**
 * Patron Chain of Responsibility: cada eslabon decide si un ticket debe escalarse por su
 * propio criterio; si no aplica, delega al siguiente eslabon de la cadena. Ningun eslabon
 * conoce a los demas mas que a traves de "next" -- se pueden agregar, quitar o reordenar
 * criterios de escalado sin tocar los eslabones existentes.
 *
 * Esta cadena es funcionalidad nueva: en E3 el estado ESCALADO existia en el enum
 * TicketStatus pero ningun camino del codigo lo alcanzaba automaticamente.
 */
public abstract class EscalationHandler {

    private EscalationHandler next;

    public EscalationHandler linkWith(EscalationHandler next) {
        this.next = next;
        return next;
    }

    public final Optional<String> handle(Ticket ticket) {
        Optional<String> reason = evaluate(ticket);
        if (reason.isPresent()) {
            return reason;
        }
        return next != null ? next.handle(ticket) : Optional.empty();
    }

    /** @return el motivo de escalado si este eslabon decide escalar, vacio si no le compete. */
    protected abstract Optional<String> evaluate(Ticket ticket);
}
