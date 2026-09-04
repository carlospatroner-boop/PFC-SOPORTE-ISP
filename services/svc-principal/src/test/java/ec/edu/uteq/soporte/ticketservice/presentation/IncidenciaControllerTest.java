package ec.edu.uteq.soporte.ticketservice.presentation;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.IncidenciaRepository;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.presentation.dto.ApiResponse;
import ec.edu.uteq.soporte.ticketservice.presentation.dto.IncidenciaResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba el endpoint de lectura de Incidencias (consumido por
 * experimentos/analizar_correlacion.py para comparar el agrupamiento real contra la verdad de
 * campo). El comportamiento propio de este controlador -- no delegado al repositorio -- es
 * cual metodo llama segun si "zone" viene o no en la peticion.
 */
@ExtendWith(MockitoExtension.class)
class IncidenciaControllerTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    private IncidenciaController controller() {
        return new IncidenciaController(incidenciaRepository);
    }

    private Incidencia incidencia() {
        return Incidencia.builder()
                .id(UUID.randomUUID())
                .zone(Zone.QUEVEDO_CENTRO)
                .createdAt(OffsetDateTime.now())
                .correlMode("c2")
                .build();
    }

    @Test
    void listIncidencias_sinZona_consultaFindAll() {
        when(incidenciaRepository.findAll()).thenReturn(List.of(incidencia()));

        ApiResponse<List<IncidenciaResponse>> response = controller().listIncidencias(null);

        verify(incidenciaRepository).findAll();
        verify(incidenciaRepository, never()).findByZone(any());
        assertThat(response.data()).hasSize(1);
        assertThat(response.message()).isEqualTo("OK");
    }

    @Test
    void listIncidencias_conZona_consultaFindByZoneEnVezDeFindAll() {
        when(incidenciaRepository.findByZone(Zone.QUEVEDO_SUR)).thenReturn(List.of(incidencia()));

        ApiResponse<List<IncidenciaResponse>> response = controller().listIncidencias(Zone.QUEVEDO_SUR);

        verify(incidenciaRepository).findByZone(Zone.QUEVEDO_SUR);
        verify(incidenciaRepository, never()).findAll();
        assertThat(response.data()).hasSize(1);
    }

    @Test
    void listIncidencias_traduceCadaIncidenciaAlDtoDeRespuesta() {
        Incidencia i = incidencia();
        when(incidenciaRepository.findAll()).thenReturn(List.of(i));

        ApiResponse<List<IncidenciaResponse>> response = controller().listIncidencias(null);

        IncidenciaResponse dto = response.data().get(0);
        assertThat(dto.incidenciaId()).isEqualTo(i.getId());
        assertThat(dto.zone()).isEqualTo(i.getZone());
        assertThat(dto.correlMode()).isEqualTo("c2");
    }

    @Test
    void listIncidencias_listaVaciaDevuelveDataVaciaNoNull() {
        when(incidenciaRepository.findAll()).thenReturn(List.of());

        ApiResponse<List<IncidenciaResponse>> response = controller().listIncidencias(null);

        assertThat(response.data()).isEmpty();
    }
}
