package ec.edu.uteq.soporte.ticketservice.application.command;

import ec.edu.uteq.soporte.ticketservice.application.ForbiddenException;
import ec.edu.uteq.soporte.ticketservice.application.TicketAuthorization;
import ec.edu.uteq.soporte.ticketservice.application.TicketWriter;
import ec.edu.uteq.soporte.ticketservice.domain.EventPublisher;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.domain.factory.TicketFactory;
import ec.edu.uteq.soporte.ticketservice.domain.policy.DefaultSlaPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria pura del manejador del comando CreateTicketCommand -- usa dobles de
 * prueba de los puertos (EventPublisher) y de las demas dependencias de aplicacion, ninguna
 * infraestructura real (Modulo A, item 5 de la guia de E4).
 */
@ExtendWith(MockitoExtension.class)
class CreateTicketHandlerTest {

    private final TicketAuthorization authorization = new TicketAuthorization();
    private final TicketFactory ticketFactory = new TicketFactory(new DefaultSlaPolicy());

    @Mock
    private TicketWriter ticketWriter;

    @Mock
    private EventPublisher eventPublisher;

    @Test
    void createTicket_persistsWithNuevoStatusAndDefaultSla() {
        CreateTicketHandler handler = new CreateTicketHandler(authorization, ticketFactory, ticketWriter, eventPublisher);
        when(ticketWriter.saveWithRetry(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTicketCommand command = new CreateTicketCommand(
                Zone.QUEVEDO_NORTE, "Sin acceso a Internet", "El router muestra luz roja",
                "0991234567", "Av. Quevedo 123", UUID.randomUUID(), "CLIENTE");

        Ticket result = handler.handle(command);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.NUEVO);
        assertThat(result.getZone()).isEqualTo(Zone.QUEVEDO_NORTE);
        assertThat(result.getSlaDeadline()).isAfter(result.getCreatedAt());
        assertThat(result.getId()).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Sin acceso a Internet -- El router muestra luz roja");
    }

    @Test
    void createTicket_publishesTicketCreatedEvent() {
        CreateTicketHandler handler = new CreateTicketHandler(authorization, ticketFactory, ticketWriter, eventPublisher);
        when(ticketWriter.saveWithRetry(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTicketCommand command = new CreateTicketCommand(
                Zone.QUEVEDO_CENTRO, "Sin internet", "No hay servicio desde ayer", null, null,
                UUID.randomUUID(), "CLIENTE");

        Ticket result = handler.handle(command);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher).publish(topicCaptor.capture(), keyCaptor.capture(), any());

        assertThat(topicCaptor.getValue()).isEqualTo("ticket.created");
        assertThat(keyCaptor.getValue()).isEqualTo(result.getId().toString());
    }

    @Test
    void createTicket_asAdmin_isAllowed() {
        CreateTicketHandler handler = new CreateTicketHandler(authorization, ticketFactory, ticketWriter, eventPublisher);
        when(ticketWriter.saveWithRetry(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTicketCommand command = new CreateTicketCommand(
                Zone.QUEVEDO_NORTE, "Titulo", "Descripcion", null, null, UUID.randomUUID(), "ADMIN");

        assertThat(handler.handle(command)).isNotNull();
    }

    @Test
    void createTicket_asTecnico_isForbidden() {
        CreateTicketHandler handler = new CreateTicketHandler(authorization, ticketFactory, ticketWriter, eventPublisher);
        CreateTicketCommand command = new CreateTicketCommand(
                Zone.QUEVEDO_NORTE, "Titulo", "Descripcion", null, null, UUID.randomUUID(), "TECNICO");

        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(ForbiddenException.class);
    }
}
