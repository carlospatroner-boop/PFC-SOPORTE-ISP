package ec.edu.uteq.soporte.ticketservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba el adaptador Kafka del puerto EventPublisher: serializa el evento a JSON real (no un
 * ObjectMapper mockeado, para detectar un cambio que rompa la serializacion de verdad) y, sobre
 * todo, que un fallo -- de Kafka o de serializacion -- nunca se propaga hacia quien publico
 * (postura AP de ADR-0004: un abonado debe poder reportar una averia aunque la mensajeria este
 * degradada).
 */
@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherAdapterTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private KafkaEventPublisherAdapter adapter() {
        return new KafkaEventPublisherAdapter(kafkaTemplate, objectMapper);
    }

    @Test
    void publish_envuiaElEventoSerializadoComoJsonAlTopicoYClaveIndicados() {
        record Evento(String ticketId, String status) {}

        adapter().publish("ticket.status-changed", "abc-123", new Evento("abc-123", "ESCALADO"));

        verify(kafkaTemplate).send(
                "ticket.status-changed",
                "abc-123",
                "{\"ticketId\":\"abc-123\",\"status\":\"ESCALADO\"}");
    }

    @Test
    void publish_unFalloDeKafkaTemplateNuncaPropagaLaExcepcion() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("simulado: broker de Kafka no disponible"));

        assertThatCode(() -> adapter().publish("ticket.created", "id-1", Map.of("a", 1)))
                .doesNotThrowAnyException();
    }

    @Test
    void publish_unEventoNoSerializableNuncaPropagaLaExcepcion() {
        Object noSerializable = new Object() {
            // Jackson no puede serializar una instancia anonima sin propiedades legibles
            // ademas de "class" -- ObjectMapper por defecto falla ante esto con
            // InvalidDefinitionException.
        };

        assertThatCode(() -> adapter().publish("ticket.created", "id-2", noSerializable))
                .doesNotThrowAnyException();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}
