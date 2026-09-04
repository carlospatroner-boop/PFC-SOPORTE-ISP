package ec.edu.uteq.soporte.ticketservice.application.correlation;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.IncidenciaRepository;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.domain.correlation.CorrelationStrategy;
import ec.edu.uteq.soporte.ticketservice.domain.correlation.SinCorrelacionStrategy;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba el orquestador CorrelationService: resolucion de la estrategia activa por
 * CORREL en el constructor, y que un fallo durante la correlacion (incluida una
 * excepcion propagada desde la estrategia o el repositorio) nunca se propaga hacia
 * arriba -- mismo principio de ADR-0004 que ya cubre CreateTicketHandlerTest para el
 * publish de Kafka.
 */
@ExtendWith(MockitoExtension.class)
class CorrelationServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    private Ticket ticket() {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .zone(Zone.QUEVEDO_NORTE)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void constructor_resuelveLaEstrategiaActivaSegunCorrelMode() {
        CorrelationStrategy c0 = new SinCorrelacionStrategy();
        Map<String, CorrelationStrategy> estrategias = Map.of("c0", c0);

        CorrelationService service =
                new CorrelationService(estrategias, incidenciaRepository, "c0", 15L);

        when(incidenciaRepository.findByZoneAndCreatedAtAfter(any(), any())).thenReturn(List.of());
        Ticket ticket = ticket();

        service.correlacionar(ticket);

        verify(incidenciaRepository).save(any(Incidencia.class));
    }

    @Test
    void constructor_lanzaExcepcionSiCorrelModeNoCoincideConNingunaEstrategiaRegistrada() {
        Map<String, CorrelationStrategy> estrategias = Map.of("c0", new SinCorrelacionStrategy());

        assertThatThrownBy(() ->
                new CorrelationService(estrategias, incidenciaRepository, "c9-inexistente", 15L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("c9-inexistente");
    }

    @Test
    void correlacionar_consultaCandidatasDentroDeLaVentanaConfigurada() {
        CorrelationStrategy c0 = new SinCorrelacionStrategy();
        CorrelationService service =
                new CorrelationService(Map.of("c0", c0), incidenciaRepository, "c0", 30L);
        when(incidenciaRepository.findByZoneAndCreatedAtAfter(any(), any())).thenReturn(List.of());
        Ticket ticket = ticket();

        service.correlacionar(ticket);

        verify(incidenciaRepository)
                .findByZoneAndCreatedAtAfter(eq(Zone.QUEVEDO_NORTE), eq(ticket.getCreatedAt().minusMinutes(30)));
    }

    @Test
    void correlacionar_unFalloDeLaEstrategiaNuncaPropagaLaExcepcion() {
        CorrelationStrategy estrategiaQueFalla = (t, candidatas) -> {
            throw new RuntimeException("simulado: fallo interno de la estrategia");
        };
        CorrelationService service = new CorrelationService(
                Map.of("c0", estrategiaQueFalla), incidenciaRepository, "c0", 15L);
        when(incidenciaRepository.findByZoneAndCreatedAtAfter(any(), any())).thenReturn(List.of());

        // No debe lanzar -- la creacion del ticket nunca depende de que esto funcione.
        service.correlacionar(ticket());

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void correlacionar_unFalloAlGuardarLaIncidenciaTampocoPropagaLaExcepcion() {
        CorrelationStrategy c0 = new SinCorrelacionStrategy();
        CorrelationService service =
                new CorrelationService(Map.of("c0", c0), incidenciaRepository, "c0", 15L);
        when(incidenciaRepository.findByZoneAndCreatedAtAfter(any(), any())).thenReturn(List.of());
        when(incidenciaRepository.save(any())).thenThrow(new RuntimeException("simulado: CockroachDB caido"));

        service.correlacionar(ticket());

        verify(incidenciaRepository, times(1)).save(any());
    }
}
