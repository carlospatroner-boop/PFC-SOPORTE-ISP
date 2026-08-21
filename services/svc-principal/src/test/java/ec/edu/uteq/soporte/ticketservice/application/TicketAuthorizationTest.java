package ec.edu.uteq.soporte.ticketservice.application;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketAuthorizationTest {

    private final TicketAuthorization authorization = new TicketAuthorization();

    private Ticket ticketIn(Zone zone) {
        return Ticket.builder()
                .zone(zone).id(UUID.randomUUID()).clientId(UUID.randomUUID())
                .status(TicketStatus.NUEVO).createdAt(OffsetDateTime.now()).build();
    }

    @Test
    void assertCanView_admin_neverForbidden() {
        assertThatCode(() -> authorization.assertCanView(ticketIn(Zone.QUEVEDO_SUR), "ADMIN", UUID.randomUUID(), null))
                .doesNotThrowAnyException();
    }

    @Test
    void assertCanView_tecnicoInOwnZone_allowed() {
        Ticket ticket = ticketIn(Zone.QUEVEDO_NORTE);
        assertThatCode(() -> authorization.assertCanView(ticket, "TECNICO", UUID.randomUUID(), Zone.QUEVEDO_NORTE))
                .doesNotThrowAnyException();
    }

    @Test
    void assertCanManage_unknownRole_isForbidden() {
        Ticket ticket = ticketIn(Zone.QUEVEDO_NORTE);
        assertThatThrownBy(() -> authorization.assertCanManage(ticket, "DESCONOCIDO", null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assertCanCreate_cliente_isAllowed() {
        assertThatCode(() -> authorization.assertCanCreate("CLIENTE")).doesNotThrowAnyException();
    }

    @Test
    void assertCanCreate_tecnico_isForbidden() {
        assertThatThrownBy(() -> authorization.assertCanCreate("TECNICO"))
                .isInstanceOf(ForbiddenException.class);
    }
}
