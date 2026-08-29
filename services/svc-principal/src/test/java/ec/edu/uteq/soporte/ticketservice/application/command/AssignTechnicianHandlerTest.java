package ec.edu.uteq.soporte.ticketservice.application.command;

import ec.edu.uteq.soporte.ticketservice.application.ForbiddenException;
import ec.edu.uteq.soporte.ticketservice.application.TicketAuthorization;
import ec.edu.uteq.soporte.ticketservice.application.TicketWriter;
import ec.edu.uteq.soporte.ticketservice.domain.EventPublisher;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketRepository;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignTechnicianHandlerTest {

    private final TicketAuthorization authorization = new TicketAuthorization();

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketWriter ticketWriter;

    @Mock
    private EventPublisher eventPublisher;

    private AssignTechnicianHandler handler() {
        return new AssignTechnicianHandler(ticketRepository, authorization, ticketWriter, eventPublisher);
    }

    @Test
    void assignTechnician_byTecnicoInAnotherZone_isForbidden() {
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_CENTRO, id);
        when(ticketRepository.findByTicketId(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> handler().handle(
                new AssignTechnicianCommand(id, UUID.randomUUID(), "TECNICO", Zone.QUEVEDO_NORTE)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assignTechnician_publishesTicketAssignedEvent() {
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_NORTE, id);
        UUID technicianId = UUID.randomUUID();
        when(ticketRepository.findByTicketId(id)).thenReturn(Optional.of(existing));
        when(ticketWriter.saveWithRetry(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket result = handler().handle(new AssignTechnicianCommand(id, technicianId, "ADMIN", null));

        assertThat(result.getStatus()).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(result.getTechnicianId()).isEqualTo(technicianId);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher).publish(topicCaptor.capture(), any(), any());
        assertThat(topicCaptor.getValue()).isEqualTo("ticket.assigned");
    }

    private Ticket ticketIn(Zone zone, UUID id) {
        return Ticket.builder()
                .zone(zone)
                .id(id)
                .clientId(UUID.randomUUID())
                .status(TicketStatus.NUEVO)
                .createdAt(OffsetDateTime.now())
                .slaDeadline(OffsetDateTime.now().plusHours(24))
                .build();
    }
}
