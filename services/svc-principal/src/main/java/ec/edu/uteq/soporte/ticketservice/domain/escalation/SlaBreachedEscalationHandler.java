package ec.edu.uteq.soporte.ticketservice.domain.escalation;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/** Primer eslabon: cualquier ticket con el SLA ya vencido que siga activo debe escalarse. */
@Component
@Order(1)
public class SlaBreachedEscalationHandler extends EscalationHandler {

    private static final Set<TicketStatus> ESTADOS_TERMINALES_O_YA_ESCALADOS =
            Set.of(TicketStatus.RESUELTO, TicketStatus.CERRADO, TicketStatus.ESCALADO);

    @Override
    protected Optional<String> evaluate(Ticket ticket) {
        if (ESTADOS_TERMINALES_O_YA_ESCALADOS.contains(ticket.getStatus())) {
            return Optional.empty();
        }
        if (ticket.getSlaDeadline() != null && ticket.getSlaDeadline().isBefore(java.time.OffsetDateTime.now())) {
            return Optional.of("SLA vencido sin resolucion");
        }
        return Optional.empty();
    }
}
