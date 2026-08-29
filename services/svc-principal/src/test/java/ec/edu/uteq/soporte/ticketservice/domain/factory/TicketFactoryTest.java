package ec.edu.uteq.soporte.ticketservice.domain.factory;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.domain.policy.DefaultSlaPolicy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketFactoryTest {

    private final TicketFactory factory = new TicketFactory(new DefaultSlaPolicy());

    @Test
    void crearNuevo_generaIdEstadoNuevoYSlaDe24Horas() {
        UUID clientId = UUID.randomUUID();

        Ticket ticket = factory.crearNuevo(Zone.QUEVEDO_SUR, clientId, "Sin servicio");

        assertThat(ticket.getId()).isNotNull();
        assertThat(ticket.getZone()).isEqualTo(Zone.QUEVEDO_SUR);
        assertThat(ticket.getClientId()).isEqualTo(clientId);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.NUEVO);
        assertThat(ticket.getDescription()).isEqualTo("Sin servicio");
        assertThat(ticket.isSlaBreached()).isFalse();
        assertThat(ticket.getSlaDeadline()).isEqualTo(ticket.getCreatedAt().plusHours(24));
    }

    @Test
    void crearNuevo_generaIdsDistintosEnCadaLlamada() {
        Ticket a = factory.crearNuevo(Zone.QUEVEDO_NORTE, UUID.randomUUID(), "A");
        Ticket b = factory.crearNuevo(Zone.QUEVEDO_NORTE, UUID.randomUUID(), "B");

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }
}
