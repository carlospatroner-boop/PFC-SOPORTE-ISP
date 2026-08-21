package ec.edu.uteq.soporte.ticketservice.infrastructure.scheduling;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.EscalationObserver;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * Observador concreto: deja constancia en el log de cada escalado automatico. Es el
 * comportamiento que antes vivia en linea dentro de EscalationScheduler.
 */
@Component
public class EscalationLoggingObserver implements EscalationObserver {

    private static final Logger LOGGER = Logger.getLogger(EscalationLoggingObserver.class.getName());

    @Override
    public void onTicketEscalated(Ticket ticket, String motivo) {
        LOGGER.info("Ticket " + ticket.getId() + " escalado automaticamente: " + motivo);
    }
}
