package ec.edu.uteq.soporte.ticketservice.infrastructure.messaging;

import ec.edu.uteq.soporte.ticketservice.domain.EventPublisher;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.domain.event.TicketEscalatedEvent;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Prueba el observador que publica "ticket.escalated" en Kafka (Saga por coreografia,
 * ADR-0004), disparado desde el escalado automatico -- un evento que antes de la Entrega 4
 * nunca ocurria porque ESCALADO era un estado inalcanzable.
 */
@ExtendWith(MockitoExtension.class)
class EscalationEventPublisherObserverTest {

    @Mock
    private EventPublisher eventPublisher;

    @Test
    void onTicketEscalated_publicaEnElTopicoTicketEscalatedConElPayloadCorrecto() {
        EscalationEventPublisherObserver observer = new EscalationEventPublisherObserver(eventPublisher);
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .zone(Zone.QUEVEDO_NORTE)
                .createdAt(OffsetDateTime.now())
                .build();

        observer.onTicketEscalated(ticket, "SLA vencido");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("ticket.escalated");
        assertThat(keyCaptor.getValue()).isEqualTo(ticketId.toString());
        assertThat(eventCaptor.getValue()).isInstanceOf(TicketEscalatedEvent.class);
        TicketEscalatedEvent event = (TicketEscalatedEvent) eventCaptor.getValue();
        assertThat(event.ticketId()).isEqualTo(ticketId.toString());
        assertThat(event.zone()).isEqualTo("QUEVEDO_NORTE");
        assertThat(event.motivo()).isEqualTo("SLA vencido");
    }
}
