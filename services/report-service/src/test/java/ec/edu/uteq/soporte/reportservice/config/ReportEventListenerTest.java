package ec.edu.uteq.soporte.reportservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;
import ec.edu.uteq.soporte.reportservice.repository.TicketSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias puras del lado de escritura del CQRS: dado un mensaje ya
 * parseado de cada uno de los 4 topicos, confirma que ticket_summary se actualiza
 * correctamente -- sin necesidad de Kafka ni del cluster real levantados.
 */
@ExtendWith(MockitoExtension.class)
class ReportEventListenerTest {

    @Mock
    private TicketSummaryRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void onTicketCreated_savesNewSummaryRow() {
        ReportEventListener listener = new ReportEventListener(repository, objectMapper);
        UUID ticketId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        String payload = """
                {"ticketId":"%s","zone":"QUEVEDO_NORTE","clientId":"%s","description":"Sin internet","createdAt":"2026-07-25T10:00:00Z"}
                """.formatted(ticketId, clientId).strip();

        ArgumentCaptor<TicketSummary> captor = ArgumentCaptor.forClass(TicketSummary.class);
        listener.onTicketCreated(payload);
        verify(repository).save(captor.capture());

        TicketSummary saved = captor.getValue();
        assertThat(saved.getTicketId()).isEqualTo(ticketId);
        assertThat(saved.getZone()).isEqualTo("QUEVEDO_NORTE");
        assertThat(saved.getClientId()).isEqualTo(clientId);
        assertThat(saved.getStatus()).isEqualTo("NUEVO");
        assertThat(saved.getDescription()).isEqualTo("Sin internet");
    }

    @Test
    void onTicketCreated_malformedPayloadIsIgnoredWithoutThrowing() {
        ReportEventListener listener = new ReportEventListener(repository, objectMapper);

        listener.onTicketCreated("esto no es json"); // no debe lanzar

        verify(repository, never()).save(any());
    }

    @Test
    void onTicketClassified_updatesCategoryAndPriority() {
        ReportEventListener listener = new ReportEventListener(repository, objectMapper);
        UUID ticketId = UUID.randomUUID();
        TicketSummary existing = TicketSummary.builder()
                .zone("QUEVEDO_SUR")
                .ticketId(ticketId)
                .status("NUEVO")
                .createdAt(OffsetDateTime.now())
                .build();
        when(repository.findById(any())).thenReturn(Optional.of(existing));
        ArgumentCaptor<TicketSummary> captor = ArgumentCaptor.forClass(TicketSummary.class);

        String payload = """
                {"ticketId":"%s","zone":"QUEVEDO_SUR","category":"CONECTIVIDAD","priority":"CRITICO"}
                """.formatted(ticketId).strip();
        listener.onTicketClassified(payload);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("CONECTIVIDAD");
        assertThat(captor.getValue().getPriority()).isEqualTo("CRITICO");
    }

    @Test
    void onTicketClassified_beforeTicketCreated_createsProvisionalRow() {
        // Condicion de carrera real entre topicos distintos (ver comentario de la
        // clase): ai-service puede clasificar y publicar tan rapido que
        // "ticket.classified" se procese antes que "ticket.created". El listener debe
        // crear una fila provisional en vez de descartar el evento.
        ReportEventListener listener = new ReportEventListener(repository, objectMapper);
        UUID ticketId = UUID.randomUUID();
        when(repository.findById(any())).thenReturn(Optional.empty());
        ArgumentCaptor<TicketSummary> captor = ArgumentCaptor.forClass(TicketSummary.class);

        String payload = """
                {"ticketId":"%s","zone":"QUEVEDO_SUR","category":"DNS","priority":"BAJO"}
                """.formatted(ticketId).strip();
        listener.onTicketClassified(payload); // no debe lanzar

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTicketId()).isEqualTo(ticketId);
        assertThat(captor.getValue().getCategory()).isEqualTo("DNS");
        assertThat(captor.getValue().getPriority()).isEqualTo("BAJO");
    }

    @Test
    void onTicketCreated_afterAssignedAlreadyCreatedProvisionalRow_doesNotResetProgress() {
        // El orden inverso de la misma carrera: si "ticket.assigned" ya creo la fila
        // provisional (status ASIGNADO + tecnico), "ticket.created" no debe revertir
        // ese progreso al llegar despues -- solo completa los campos que le
        // corresponden (clientId/description/createdAt).
        ReportEventListener listener = new ReportEventListener(repository, objectMapper);
        UUID ticketId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        TicketSummary provisional = TicketSummary.builder()
                .zone("QUEVEDO_NORTE")
                .ticketId(ticketId)
                .status("ASIGNADO")
                .technicianId(technicianId)
                .build();
        when(repository.findById(any())).thenReturn(Optional.of(provisional));
        ArgumentCaptor<TicketSummary> captor = ArgumentCaptor.forClass(TicketSummary.class);

        String payload = """
                {"ticketId":"%s","zone":"QUEVEDO_NORTE","clientId":"%s","description":"Sin internet","createdAt":"2026-07-25T10:00:00Z"}
                """.formatted(ticketId, clientId).strip();
        listener.onTicketCreated(payload);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo(clientId);
        assertThat(captor.getValue().getStatus()).isEqualTo("ASIGNADO");
        assertThat(captor.getValue().getTechnicianId()).isEqualTo(technicianId);
    }

    @Test
    void onTicketStatusChanged_updatesStatus() {
        ReportEventListener listener = new ReportEventListener(repository, objectMapper);
        UUID ticketId = UUID.randomUUID();
        TicketSummary existing = TicketSummary.builder()
                .zone("QUEVEDO_NORTE")
                .ticketId(ticketId)
                .status("NUEVO")
                .build();
        when(repository.findById(any())).thenReturn(Optional.of(existing));
        ArgumentCaptor<TicketSummary> captor = ArgumentCaptor.forClass(TicketSummary.class);

        String payload = """
                {"ticketId":"%s","zone":"QUEVEDO_NORTE","oldStatus":"NUEVO","newStatus":"EN_PROGRESO"}
                """.formatted(ticketId).strip();
        listener.onTicketStatusChanged(payload);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("EN_PROGRESO");
    }

    @Test
    void onTicketAssigned_setsTechnicianAndStatus() {
        ReportEventListener listener = new ReportEventListener(repository, objectMapper);
        UUID ticketId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        TicketSummary existing = TicketSummary.builder()
                .zone("QUEVEDO_CENTRO")
                .ticketId(ticketId)
                .status("NUEVO")
                .build();
        when(repository.findById(any())).thenReturn(Optional.of(existing));
        ArgumentCaptor<TicketSummary> captor = ArgumentCaptor.forClass(TicketSummary.class);

        String payload = """
                {"ticketId":"%s","zone":"QUEVEDO_CENTRO","technicianId":"%s"}
                """.formatted(ticketId, technicianId).strip();
        listener.onTicketAssigned(payload);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTechnicianId()).isEqualTo(technicianId);
        assertThat(captor.getValue().getStatus()).isEqualTo("ASIGNADO");
    }
}
