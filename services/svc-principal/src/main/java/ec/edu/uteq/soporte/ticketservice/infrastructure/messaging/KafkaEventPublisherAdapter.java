package ec.edu.uteq.soporte.ticketservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.ticketservice.domain.EventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptador del puerto de salida EventPublisher usando Kafka. Un fallo de Kafka (broker
 * abajo, etc.) se registra pero NUNCA revierte ni bloquea la operacion que lo origino --
 * coherente con la postura AP para estas publicaciones (ADR-0004): un abonado debe poder
 * reportar una incidencia aunque la mensajeria este degradada.
 */
@Component
public class KafkaEventPublisherAdapter implements EventPublisher {

    private static final Logger LOGGER = Logger.getLogger(KafkaEventPublisherAdapter.class.getName());

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisherAdapter(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo publicar en " + topic + " (key=" + key + ")", e);
        }
    }
}
