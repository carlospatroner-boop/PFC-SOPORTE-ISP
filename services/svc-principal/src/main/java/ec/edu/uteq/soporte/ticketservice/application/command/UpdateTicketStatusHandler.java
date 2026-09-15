package ec.edu.uteq.soporte.ticketservice.application.command;

import ec.edu.uteq.soporte.ticketservice.application.TicketAuthorization;
import ec.edu.uteq.soporte.ticketservice.application.TicketNotFoundException;
import ec.edu.uteq.soporte.ticketservice.application.TicketWriter;
import ec.edu.uteq.soporte.ticketservice.domain.EventPublisher;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketRepository;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.event.TicketStatusChangedEvent;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class UpdateTicketStatusHandler implements TicketCommandHandler<UpdateTicketStatusCommand, Ticket> {

    private static final String TOPIC_TICKET_STATUS_CHANGED = "ticket.status-changed";

    private final TicketRepository ticketRepository;
    private final TicketAuthorization authorization;
    private final TicketWriter ticketWriter;
    private final EventPublisher eventPublisher;

    public UpdateTicketStatusHandler(
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
    public Ticket handle(UpdateTicketStatusCommand command) {
        Ticket ticket = ticketRepository.findByTicketId(command.ticketId())
                .orElseThrow(() -> new TicketNotFoundException(command.ticketId()));
        authorization.assertCanManage(ticket, command.role(), command.authZone());

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(command.newStatus());
        if (command.newStatus() == TicketStatus.RESUELTO) {
            ticket.setResolvedAt(OffsetDateTime.now());
            ticket.setSlaBreached(
                    ticket.getSlaDeadline() != null
                            && ticket.getResolvedAt().isAfter(ticket.getSlaDeadline())
            );
            // Evidencia del cierre en sitio (Entregable 10 de la guia de cierre): opcional a
            // proposito -- un ADMIN puede resolver un ticket sin pasar por el movil.
            if (command.evidencePhoto() != null) {
                ticket.setEvidencePhoto(command.evidencePhoto());
            }
            if (command.evidenceLatitude() != null) {
                ticket.setEvidenceLatitude(command.evidenceLatitude());
            }
            if (command.evidenceLongitude() != null) {
                ticket.setEvidenceLongitude(command.evidenceLongitude());
            }
        }
        Ticket saved = ticketWriter.saveWithRetry(ticket);
        publishStatusChanged(saved, oldStatus);
        return saved;
    }

    private void publishStatusChanged(Ticket ticket, TicketStatus oldStatus) {
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                ticket.getId().toString(), ticket.getZone().name(), oldStatus.name(), ticket.getStatus().name());
        eventPublisher.publish(TOPIC_TICKET_STATUS_CHANGED, event.ticketId(), event);
    }
}
