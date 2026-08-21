package ec.edu.uteq.soporte.authservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.authservice.domain.Role;
import ec.edu.uteq.soporte.authservice.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TechnicianEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishesTheTechnicianCreatedEventUnderTheTechnicianId() {
        TechnicianEventPublisher publisher = new TechnicianEventPublisher(kafkaTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        User technician = User.builder()
                .id(id)
                .email("tec@test.com")
                .fullName("Tecnico Norte")
                .role(Role.TECNICO)
                .zone("QUEVEDO_NORTE")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();

        publisher.publishCreated(technician);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("technician.created"), keyCaptor.capture(), payloadCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo(id.toString());
        assertThat(payloadCaptor.getValue()).contains(id.toString()).contains("Tecnico Norte").contains("QUEVEDO_NORTE");
    }

    @Test
    void aKafkaFailureIsSwallowedAndNeverPropagatesToTheCaller() {
        TechnicianEventPublisher publisher = new TechnicianEventPublisher(kafkaTemplate, objectMapper);
        doThrow(new RuntimeException("kafka no disponible")).when(kafkaTemplate).send(any(), any(), any());
        User technician = User.builder()
                .id(UUID.randomUUID())
                .email("tec@test.com")
                .fullName("Tecnico Norte")
                .role(Role.TECNICO)
                .zone("QUEVEDO_NORTE")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();

        publisher.publishCreated(technician); // no debe lanzar
    }
}
