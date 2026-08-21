package ec.edu.uteq.soporte.ticketservice.domain;

/**
 * Puerto de salida para publicar eventos de integracion (Saga por coreografia, ver
 * ADR-0004). El dominio y la capa de aplicacion solo conocen "hay que publicar este evento
 * bajo esta clave", no que el transporte es Kafka -- eso es un detalle de infraestructura
 * (ver infrastructure/messaging/KafkaEventPublisherAdapter.java).
 */
public interface EventPublisher {
    void publish(String topic, String key, Object event);
}
