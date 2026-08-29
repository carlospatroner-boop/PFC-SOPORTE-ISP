package ec.edu.uteq.soporte.ticketservice.domain.escalation;

import ec.edu.uteq.soporte.ticketservice.domain.Priority;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba la cadena completa (Chain of Responsibility, Modulo A item 4 -- "escalado" sugerido
 * para ACC) ensamblada con sus dos eslabones reales, no con dobles: lo que se verifica es
 * precisamente que la delegacion entre eslabones funciona.
 */
class EscalationChainTest {

    private final EscalationChain chain = new EscalationChain(
            List.of(new SlaBreachedEscalationHandler(), new StaleCriticalEscalationHandler()));

    @Test
    void ticketConSlaVencidoYActivo_seEscala() {
        Ticket ticket = ticketBase(Zone.QUEVEDO_NORTE);
        ticket.setStatus(TicketStatus.EN_PROGRESO);
        ticket.setSlaDeadline(OffsetDateTime.now().minusMinutes(1));

        Optional<String> motivo = chain.evaluate(ticket);

        assertThat(motivo).isPresent();
        assertThat(motivo.get()).contains("SLA vencido");
    }

    @Test
    void ticketYaResuelto_noSeEscalaAunqueElSlaEsteVencido() {
        Ticket ticket = ticketBase(Zone.QUEVEDO_NORTE);
        ticket.setStatus(TicketStatus.RESUELTO);
        ticket.setSlaDeadline(OffsetDateTime.now().minusHours(1));

        assertThat(chain.evaluate(ticket)).isEmpty();
    }

    @Test
    void ticketCriticoSinAsignarPorMasDeDosHoras_seEscalaAunqueSlaNoHayaVencido() {
        Ticket ticket = ticketBase(Zone.QUEVEDO_SUR);
        ticket.setStatus(TicketStatus.NUEVO);
        ticket.setPriority(Priority.CRITICO);
        ticket.setCreatedAt(OffsetDateTime.now().minusHours(3));
        ticket.setSlaDeadline(OffsetDateTime.now().plusHours(1)); // SLA formal aun no vence

        Optional<String> motivo = chain.evaluate(ticket);

        assertThat(motivo).isPresent();
        assertThat(motivo.get()).contains("critica sin asignar");
    }

    @Test
    void ticketNormalDentroDePlazo_noSeEscala() {
        Ticket ticket = ticketBase(Zone.QUEVEDO_CENTRO);
        ticket.setStatus(TicketStatus.NUEVO);
        ticket.setSlaDeadline(OffsetDateTime.now().plusHours(20));

        assertThat(chain.evaluate(ticket)).isEmpty();
    }

    private Ticket ticketBase(Zone zone) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .zone(zone)
                .clientId(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
