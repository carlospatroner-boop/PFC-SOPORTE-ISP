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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTicketStatusHandlerTest {

    private final TicketAuthorization authorization = new TicketAuthorization();

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketWriter ticketWriter;

    @Mock
    private EventPublisher eventPublisher;

    private UpdateTicketStatusHandler handler() {
        return new UpdateTicketStatusHandler(ticketRepository, authorization, ticketWriter, eventPublisher);
    }

    @Test
    void updateStatus_toResuelto_marksResolvedAtAndEvaluatesSlaBreach() {
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_SUR, id);
        existing.setStatus(TicketStatus.EN_PROGRESO);
        existing.setCreatedAt(OffsetDateTime.now().minusHours(30));
        existing.setSlaDeadline(OffsetDateTime.now().minusHours(6)); // ya vencido

        when(ticketRepository.findByTicketId(id)).thenReturn(Optional.of(existing));
        when(ticketWriter.saveWithRetry(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket result = handler().handle(new UpdateTicketStatusCommand(id, TicketStatus.RESUELTO, "ADMIN", null));

        assertThat(result.getStatus()).isEqualTo(TicketStatus.RESUELTO);
        assertThat(result.getResolvedAt()).isNotNull();
        assertThat(result.isSlaBreached()).isTrue();
    }

    @Test
    void updateStatus_byTecnicoInOwnZone_isAllowed() {
        UUID id = UUID.randomUUID();
        Zone zone = Zone.QUEVEDO_NORTE;
        Ticket existing = ticketIn(zone, id);
        when(ticketRepository.findByTicketId(id)).thenReturn(Optional.of(existing));
        when(ticketWriter.saveWithRetry(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket result = handler().handle(new UpdateTicketStatusCommand(id, TicketStatus.ASIGNADO, "TECNICO", zone));

        assertThat(result.getStatus()).isEqualTo(TicketStatus.ASIGNADO);
    }

    @Test
    void updateStatus_byTecnicoInAnotherZone_isForbidden() {
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_NORTE, id);
        when(ticketRepository.findByTicketId(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> handler().handle(
                new UpdateTicketStatusCommand(id, TicketStatus.ASIGNADO, "TECNICO", Zone.QUEVEDO_SUR)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateStatus_byTecnicoWithNoZone_isForbidden() {
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_NORTE, id);
        when(ticketRepository.findByTicketId(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> handler().handle(
                new UpdateTicketStatusCommand(id, TicketStatus.ASIGNADO, "TECNICO", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateStatus_byCliente_isForbidden() {
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_NORTE, id);
        when(ticketRepository.findByTicketId(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> handler().handle(
                new UpdateTicketStatusCommand(id, TicketStatus.ASIGNADO, "CLIENTE", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateStatus_publishesTicketStatusChangedEvent() {
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_NORTE, id);
        existing.setStatus(TicketStatus.NUEVO);
        when(ticketRepository.findByTicketId(id)).thenReturn(Optional.of(existing));
        when(ticketWriter.saveWithRetry(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket result = handler().handle(new UpdateTicketStatusCommand(id, TicketStatus.ASIGNADO, "ADMIN", null));

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(eventPublisher).publish(topicCaptor.capture(), any(), any());
        assertThat(topicCaptor.getValue()).isEqualTo("ticket.status-changed");
        assertThat(result.getStatus()).isEqualTo(TicketStatus.ASIGNADO);
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
