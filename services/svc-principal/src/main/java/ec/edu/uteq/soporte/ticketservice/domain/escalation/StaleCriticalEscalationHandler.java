package ec.edu.uteq.soporte.ticketservice.domain.escalation;

import ec.edu.uteq.soporte.ticketservice.domain.Priority;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Segundo eslabon: un ticket CRITICO que sigue en NUEVO (sin asignar) por mas de 2 horas
 * indica que nadie lo esta atendiendo -- se escala aunque su SLA formal no haya vencido
 * todavia, porque para prioridad critica esperar al vencimiento del SLA ya es tarde.
 */
@Component
@Order(2)
public class StaleCriticalEscalationHandler extends EscalationHandler {

    private static final Duration UMBRAL_SIN_ASIGNAR = Duration.ofHours(2);

    @Override
    protected Optional<String> evaluate(Ticket ticket) {
        if (ticket.getPriority() != Priority.CRITICO || ticket.getStatus() != TicketStatus.NUEVO) {
            return Optional.empty();
        }
        if (ticket.getCreatedAt() != null
                && ticket.getCreatedAt().isBefore(OffsetDateTime.now().minus(UMBRAL_SIN_ASIGNAR))) {
            return Optional.of("Prioridad critica sin asignar por mas de 2 horas");
        }
        return Optional.empty();
    }
}
