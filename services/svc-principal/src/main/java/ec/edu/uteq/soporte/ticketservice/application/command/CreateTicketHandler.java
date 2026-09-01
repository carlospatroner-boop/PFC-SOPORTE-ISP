package ec.edu.uteq.soporte.ticketservice.application.command;

import ec.edu.uteq.soporte.ticketservice.application.TicketAuthorization;
import ec.edu.uteq.soporte.ticketservice.application.TicketWriter;
import ec.edu.uteq.soporte.ticketservice.application.correlation.CorrelationService;
import ec.edu.uteq.soporte.ticketservice.domain.EventPublisher;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.event.TicketCreatedEvent;
import ec.edu.uteq.soporte.ticketservice.domain.factory.TicketFactory;
import org.springframework.stereotype.Component;

/**
 * Crea un ticket en estado NUEVO, sin categoria ni prioridad todavia: en la arquitectura
 * completa del PFC (Saga por coreografia), esos campos los completa ai-service de forma
 * asincrona via Kafka al consumir el evento ticket.created (ver
 * infrastructure/messaging/TicketClassificationListener). Este manejador, por ahora, solo
 * persiste el ticket y deja pendiente esa clasificacion.
 */
@Component
public class CreateTicketHandler implements TicketCommandHandler<CreateTicketCommand, Ticket> {

    private static final String TOPIC_TICKET_CREATED = "ticket.created";

    private final TicketAuthorization authorization;
    private final TicketFactory ticketFactory;
    private final TicketWriter ticketWriter;
    private final EventPublisher eventPublisher;
    private final CorrelationService correlationService;

    public CreateTicketHandler(
            TicketAuthorization authorization,
            TicketFactory ticketFactory,
            TicketWriter ticketWriter,
            EventPublisher eventPublisher,
            CorrelationService correlationService) {
        this.authorization = authorization;
        this.ticketFactory = ticketFactory;
        this.ticketWriter = ticketWriter;
        this.eventPublisher = eventPublisher;
        this.correlationService = correlationService;
    }

    @Override
    public Ticket handle(CreateTicketCommand command) {
        authorization.assertCanCreate(command.role());

        Ticket ticket = ticketFactory.crearNuevo(
                command.zone(), command.clientId(), buildDescription(command));
        Ticket saved = ticketWriter.saveWithRetry(ticket);
        publishTicketCreated(saved);
        // Adicion 1 (Ampliacion del Modulo G): decide si "saved" se agrupa en una Incidencia
        // existente o abre una nueva -- ver CorrelationService. Nunca bloquea ni revierte la
        // creacion del ticket, mismo principio que el publish de Kafka de arriba.
        correlationService.correlacionar(saved);
        return saved;
    }

    private String buildDescription(CreateTicketCommand command) {
        return command.title() + " -- " + command.description();
    }

    // Se publica en el mismo hilo, luego de guardar el ticket. Un fallo de Kafka NUNCA
    // revierte ni bloquea la creacion del ticket (ver EventPublisher/ADR-0004).
    private void publishTicketCreated(Ticket ticket) {
        TicketCreatedEvent event = new TicketCreatedEvent(
                ticket.getId().toString(), ticket.getZone().name(), ticket.getClientId().toString(),
                ticket.getDescription(), ticket.getCreatedAt().toString());
        eventPublisher.publish(TOPIC_TICKET_CREATED, event.ticketId(), event);
    }
}
