package ec.edu.uteq.soporte.ticketservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.repository.TicketRepository;
import ec.edu.uteq.soporte.ticketservice.web.dto.CreateTicketRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria pura (sin contexto de Spring, sin base de datos real) para no
 * depender de que el cluster CockroachDB este levantado al correr `mvn test`.
 * Las pruebas de integracion contra el cluster real van en tests/integration/
 * (ver estructura del repositorio, Listing 2 de la guia de la Entrega 3).
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createTicket_persistsWithNuevoStatusAndDefaultSla() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateTicketRequest request = new CreateTicketRequest(
                Zone.QUEVEDO_NORTE,
                "Sin acceso a Internet",
                "El router muestra luz roja",
                "0991234567",
                "Av. Quevedo 123"
        );

        Ticket result = service.createTicket(request, UUID.randomUUID(), "CLIENTE");

        assertThat(result.getStatus()).isEqualTo(TicketStatus.NUEVO);
        assertThat(result.getZone()).isEqualTo(Zone.QUEVEDO_NORTE);
        assertThat(result.getSlaDeadline()).isAfter(result.getCreatedAt());
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void createTicket_publishesTicketCreatedEvent() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTicketRequest request = new CreateTicketRequest(
                Zone.QUEVEDO_CENTRO, "Sin internet", "No hay servicio desde ayer", null, null);

        Ticket result = service.createTicket(request, UUID.randomUUID(), "CLIENTE");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("ticket.created"), keyCaptor.capture(), payloadCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo(result.getId().toString());
        assertThat(payloadCaptor.getValue())
                .contains(result.getId().toString())
                .contains("QUEVEDO_CENTRO")
                .contains("Sin internet");
    }

    @Test
    void createTicket_asAdmin_isAllowed() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTicketRequest request = new CreateTicketRequest(
                Zone.QUEVEDO_NORTE, "Titulo", "Descripcion", null, null);

        assertThat(service.createTicket(request, UUID.randomUUID(), "ADMIN")).isNotNull();
    }

    @Test
    void createTicket_asTecnico_isForbidden() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        CreateTicketRequest request = new CreateTicketRequest(
                Zone.QUEVEDO_NORTE, "Titulo", "Descripcion", null, null);

        assertThatThrownBy(() -> service.createTicket(request, UUID.randomUUID(), "TECNICO"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateStatus_toResuelto_marksResolvedAtAndEvaluatesSlaBreach() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        Zone zone = Zone.QUEVEDO_SUR;

        Ticket existing = Ticket.builder()
                .zone(zone)
                .id(id)
                .clientId(UUID.randomUUID())
                .status(TicketStatus.EN_PROGRESO)
                .createdAt(OffsetDateTime.now().minusHours(30))
                .slaDeadline(OffsetDateTime.now().minusHours(6)) // ya vencido
                .build();

        when(ticketRepository.findById(any())).thenReturn(Optional.of(existing));
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        when(ticketRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        Ticket result = service.updateStatus(zone, id, TicketStatus.RESUELTO, "ADMIN", null);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.RESUELTO);
        assertThat(result.getResolvedAt()).isNotNull();
        assertThat(result.isSlaBreached()).isTrue(); // se resolvio despues del deadline
    }

    @Test
    void updateStatus_byTecnicoInOwnZone_isAllowed() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        Zone zone = Zone.QUEVEDO_NORTE;
        Ticket existing = ticketIn(zone, id);

        when(ticketRepository.findById(any())).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket result = service.updateStatus(zone, id, TicketStatus.ASIGNADO, "TECNICO", zone);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.ASIGNADO);
    }

    @Test
    void updateStatus_byTecnicoInAnotherZone_isForbidden() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_NORTE, id);
        when(ticketRepository.findById(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateStatus(
                Zone.QUEVEDO_NORTE, id, TicketStatus.ASIGNADO, "TECNICO", Zone.QUEVEDO_SUR))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateStatus_byTecnicoWithNoZone_isForbidden() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_NORTE, id);
        when(ticketRepository.findById(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateStatus(
                Zone.QUEVEDO_NORTE, id, TicketStatus.ASIGNADO, "TECNICO", null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateStatus_byCliente_isForbidden() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_NORTE, id);
        when(ticketRepository.findById(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateStatus(
                Zone.QUEVEDO_NORTE, id, TicketStatus.ASIGNADO, "CLIENTE", null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assignTechnician_byTecnicoInAnotherZone_isForbidden() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_CENTRO, id);
        when(ticketRepository.findById(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.assignTechnician(
                Zone.QUEVEDO_CENTRO, id, UUID.randomUUID(), "TECNICO", Zone.QUEVEDO_NORTE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getTicket_byOwningCliente_isAllowed() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_SUR, id);
        existing.setClientId(clientId);
        when(ticketRepository.findById(any())).thenReturn(Optional.of(existing));

        Ticket result = service.getTicket(Zone.QUEVEDO_SUR, id, "CLIENTE", clientId, null);

        assertThat(result.getClientId()).isEqualTo(clientId);
    }

    @Test
    void getTicket_byNonOwningCliente_isForbidden() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_SUR, id);
        existing.setClientId(UUID.randomUUID());
        when(ticketRepository.findById(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.getTicket(Zone.QUEVEDO_SUR, id, "CLIENTE", UUID.randomUUID(), null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getTicket_byTecnicoInAnotherZone_isForbidden() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        Ticket existing = ticketIn(Zone.QUEVEDO_SUR, id);
        when(ticketRepository.findById(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.getTicket(
                Zone.QUEVEDO_SUR, id, "TECNICO", UUID.randomUUID(), Zone.QUEVEDO_CENTRO))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listTickets_asCliente_onlyReturnsOwnTickets() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        UUID clientId = UUID.randomUUID();
        Ticket own = ticketIn(Zone.QUEVEDO_NORTE, UUID.randomUUID());
        own.setClientId(clientId);
        when(ticketRepository.findByClientId(clientId)).thenReturn(List.of(own));

        List<Ticket> result = service.listTickets(Zone.QUEVEDO_SUR, null, "CLIENTE", clientId, null);

        // El parametro "zone" (QUEVEDO_SUR) se ignora: siempre se filtra por clientId.
        assertThat(result).containsExactly(own);
    }

    @Test
    void listTickets_asTecnico_onlyReturnsOwnZoneIgnoringZoneParam() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);
        Ticket zoneTicket = ticketIn(Zone.QUEVEDO_NORTE, UUID.randomUUID());
        when(ticketRepository.findByZone(Zone.QUEVEDO_NORTE)).thenReturn(List.of(zoneTicket));

        List<Ticket> result = service.listTickets(
                Zone.QUEVEDO_SUR, null, "TECNICO", UUID.randomUUID(), Zone.QUEVEDO_NORTE);

        assertThat(result).containsExactly(zoneTicket);
    }

    @Test
    void listTickets_asTecnicoWithNoZone_returnsEmpty() {
        TicketService service = new TicketService(ticketRepository, kafkaTemplate, objectMapper);

        List<Ticket> result = service.listTickets(null, null, "TECNICO", UUID.randomUUID(), null);

        assertThat(result).isEmpty();
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
