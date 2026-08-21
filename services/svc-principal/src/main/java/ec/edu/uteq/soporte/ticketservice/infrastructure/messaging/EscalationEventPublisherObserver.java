package ec.edu.uteq.soporte.ticketservice.infrastructure.messaging;

import ec.edu.uteq.soporte.ticketservice.domain.EventPublisher;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.EscalationObserver;
import ec.edu.uteq.soporte.ticketservice.domain.event.TicketEscalatedEvent;
import org.springframework.stereotype.Component;

/**
 * Observador concreto: publica "ticket.escalated" en Kafka para que notification-service
 * pueda avisar a coordinadores/tecnicos y report-service actualice su modelo de lectura --
 * misma logica de Saga por coreografia que ya usan create/status-changed (ver ADR-0004),
 * ahora tambien disparada desde el escalado automatico, que antes de la Entrega 4 nunca
 * ocurria.
 */
@Component
public class EscalationEventPublisherObserver implements EscalationObserver {

    private static final String TOPIC_TICKET_ESCALATED = "ticket.escalated";

    private final EventPublisher eventPublisher;

    public EscalationEventPublisherObserver(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onTicketEscalated(Ticket ticket, String motivo) {
        TicketEscalatedEvent event = new TicketEscalatedEvent(
                ticket.getId().toString(), ticket.getZone().name(), motivo);
        eventPublisher.publish(TOPIC_TICKET_ESCALATED, event.ticketId(), event);
    }
}
