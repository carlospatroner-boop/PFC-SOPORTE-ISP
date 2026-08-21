package ec.edu.uteq.soporte.ticketservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.ticketservice.infrastructure.persistence.SpringDataTechnicianRepository;
import ec.edu.uteq.soporte.ticketservice.infrastructure.persistence.TechnicianJpaEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias puras del lado consumidor de la Saga (ver TechnicianSyncListener):
 * dado un mensaje de "technician.created" ya en texto, confirma que se crea/actualiza la
 * fila local de `technicians` -- sin Kafka real.
 */
@ExtendWith(MockitoExtension.class)
class TechnicianSyncListenerTest {

    @Mock
    private SpringDataTechnicianRepository technicianRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void creaUnaFilaNuevaCuandoElTecnicoNoExistiaTodavia() {
        TechnicianSyncListener listener = new TechnicianSyncListener(technicianRepository, objectMapper);
        UUID id = UUID.randomUUID();
        when(technicianRepository.findById(id)).thenReturn(Optional.empty());
        when(technicianRepository.save(any(TechnicianJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String payload = """
                {"technicianId":"%s","fullName":"Tecnico Norte","zone":"QUEVEDO_NORTE"}
                """.formatted(id).strip();

        listener.onTechnicianCreated(payload);

        ArgumentCaptor<TechnicianJpaEntity> captor = ArgumentCaptor.forClass(TechnicianJpaEntity.class);
        verify(technicianRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
        assertThat(captor.getValue().getFullName()).isEqualTo("Tecnico Norte");
        assertThat(captor.getValue().getZone()).isEqualTo("QUEVEDO_NORTE");
    }

    @Test
    void actualizaLaFilaExistenteEnVezDeDuplicarla() {
        TechnicianSyncListener listener = new TechnicianSyncListener(technicianRepository, objectMapper);
        UUID id = UUID.randomUUID();
        TechnicianJpaEntity existing = new TechnicianJpaEntity(id, "Nombre Viejo", "QUEVEDO_SUR", null, true);
        when(technicianRepository.findById(id)).thenReturn(Optional.of(existing));
        when(technicianRepository.save(any(TechnicianJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String payload = """
                {"technicianId":"%s","fullName":"Nombre Nuevo","zone":"QUEVEDO_NORTE"}
                """.formatted(id).strip();

        listener.onTechnicianCreated(payload);

        ArgumentCaptor<TechnicianJpaEntity> captor = ArgumentCaptor.forClass(TechnicianJpaEntity.class);
        verify(technicianRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getFullName()).isEqualTo("Nombre Nuevo");
        assertThat(captor.getValue().getZone()).isEqualTo("QUEVEDO_NORTE");
    }

    @Test
    void malformedPayloadIsIgnoredWithoutThrowing() {
        TechnicianSyncListener listener = new TechnicianSyncListener(technicianRepository, objectMapper);

        listener.onTechnicianCreated("esto no es json"); // no debe lanzar

        verify(technicianRepository, never()).save(any());
    }
}
