package ec.edu.uteq.soporte.ticketservice.infrastructure.scheduling;

import ec.edu.uteq.soporte.ticketservice.domain.Priority;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketRepository;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.EscalationChain;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.EscalationObserver;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.SlaBreachedEscalationHandler;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.StaleCriticalEscalationHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba el scheduler como "sujeto" del patron Observer: dado un ticket con SLA vencido,
 * confirma que (a) lo persiste como ESCALADO y (b) notifica a CADA observador registrado,
 * sin que el scheduler necesite saber que hace cada uno.
 */
@ExtendWith(MockitoExtension.class)
class EscalationSchedulerTest {

    private final EscalationChain escalationChain = new EscalationChain(
            List.of(new SlaBreachedEscalationHandler(), new StaleCriticalEscalationHandler()));

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EscalationObserver observerUno;

    @Mock
    private EscalationObserver observerDos;

    @Test
    void ticketConSlaVencido_seEscalaYNotificaATodosLosObservadores() {
        Ticket ticket = ticketBase();
        ticket.setStatus(TicketStatus.EN_PROGRESO);
        ticket.setSlaDeadline(OffsetDateTime.now().minusMinutes(5));

        when(ticketRepository.findByStatus(TicketStatus.NUEVO)).thenReturn(List.of());
        when(ticketRepository.findByStatus(TicketStatus.ASIGNADO)).thenReturn(List.of());
        when(ticketRepository.findByStatus(TicketStatus.EN_PROGRESO)).thenReturn(List.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        EscalationScheduler scheduler = new EscalationScheduler(
                ticketRepository, escalationChain, List.of(observerUno, observerDos));

        scheduler.evaluarEscalados();

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ESCALADO);
        verify(ticketRepository).save(ticket);
        verify(observerUno).onTicketEscalated(any(Ticket.class), any(String.class));
        verify(observerDos).onTicketEscalated(any(Ticket.class), any(String.class));
    }

    @Test
    void unObservadorQueFalla_noImpideQueLosDemasSeNotifiquenNiQueElTicketQuedeEscalado() {
        Ticket ticket = ticketBase();
        ticket.setStatus(TicketStatus.NUEVO);
        ticket.setSlaDeadline(OffsetDateTime.now().minusMinutes(5));

        when(ticketRepository.findByStatus(TicketStatus.NUEVO)).thenReturn(List.of(ticket));
        when(ticketRepository.findByStatus(TicketStatus.ASIGNADO)).thenReturn(List.of());
        when(ticketRepository.findByStatus(TicketStatus.EN_PROGRESO)).thenReturn(List.of());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("Kafka no disponible"))
                .when(observerUno).onTicketEscalated(any(Ticket.class), any(String.class));

        EscalationScheduler scheduler = new EscalationScheduler(
                ticketRepository, escalationChain, List.of(observerUno, observerDos));

        scheduler.evaluarEscalados(); // no debe lanzar

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ESCALADO);
        verify(observerDos).onTicketEscalated(any(Ticket.class), any(String.class));
    }

    @Test
    void sinTicketsQueEscalar_ningunObservadorEsNotificado() {
        when(ticketRepository.findByStatus(any(TicketStatus.class))).thenReturn(List.of());

        EscalationScheduler scheduler = new EscalationScheduler(
                ticketRepository, escalationChain, List.of(observerUno));

        scheduler.evaluarEscalados();

        verify(observerUno, never()).onTicketEscalated(any(Ticket.class), any(String.class));
        verify(ticketRepository, never()).save(any());
    }

    private Ticket ticketBase() {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .zone(Zone.QUEVEDO_NORTE)
                .clientId(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .priority(Priority.MEDIO)
                .build();
    }
}
