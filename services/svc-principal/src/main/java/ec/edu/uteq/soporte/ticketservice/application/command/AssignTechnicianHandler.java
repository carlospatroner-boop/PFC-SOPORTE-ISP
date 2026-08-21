package ec.edu.uteq.soporte.ticketservice.application.command;

import ec.edu.uteq.soporte.ticketservice.application.TicketAuthorization;
import ec.edu.uteq.soporte.ticketservice.application.TicketNotFoundException;
import ec.edu.uteq.soporte.ticketservice.application.TicketWriter;
import ec.edu.uteq.soporte.ticketservice.domain.EventPublisher;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketRepository;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.event.TicketAssignedEvent;
import org.springframework.stereotype.Component;

@Component
public class AssignTechnicianHandler implements TicketCommandHandler<AssignTechnicianCommand, Ticket> {

    private static final String TOPIC_TICKET_ASSIGNED = "ticket.assigned";

    private final TicketRepository ticketRepository;
    private final TicketAuthorization authorization;
    private final TicketWriter ticketWriter;
    private final EventPublisher eventPublisher;

    public AssignTechnicianHandler(
            TicketRepository ticketRepository,
            TicketAuthorization authorization,
            TicketWriter ticketWriter,
            EventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.authorization = authorization;
        this.ticketWriter = ticketWriter;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Ticket handle(AssignTechnicianCommand command) {
        Ticket ticket = ticketRepository.findByTicketId(command.ticketId())
                .orElseThrow(() -> new TicketNotFoundException(command.ticketId()));
        authorization.assertCanManage(ticket, command.role(), command.authZone());

        ticket.setTechnicianId(command.technicianId());
        ticket.setStatus(TicketStatus.ASIGNADO);
        Ticket saved = ticketWriter.saveWithRetry(ticket);
        publishAssigned(saved);
        return saved;
    }

    private void publishAssigned(Ticket ticket) {
        TicketAssignedEvent event = new TicketAssignedEvent(
                ticket.getId().toString(), ticket.getZone().name(), ticket.getTechnicianId().toString());
        eventPublisher.publish(TOPIC_TICKET_ASSIGNED, event.ticketId(), event);
    }
}
