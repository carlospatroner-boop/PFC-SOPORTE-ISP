package ec.edu.uteq.soporte.ticketservice.infrastructure.metrics;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el observador que registra cada escalado como metrica de negocio
 * ("app_business_events_total", ver Modulo F). Usa un SimpleMeterRegistry real (no un mock de
 * Prometheus) para verificar que el contador de verdad incrementa, con la etiqueta correcta.
 */
class EscalationMetricsObserverTest {

    private Ticket ticket() {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .zone(Zone.QUEVEDO_CENTRO)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void onTicketEscalated_incrementaElContadorDeEventosDeNegocio() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EscalationMetricsObserver observer = new EscalationMetricsObserver(registry);

        observer.onTicketEscalated(ticket(), "SLA vencido");

        double valor = registry.get("app_business_events_total")
                .tag("event", "ticket_escalated")
                .counter()
                .count();
        assertThat(valor).isEqualTo(1.0);
    }

    @Test
    void onTicketEscalated_llamadoDosVecesAcumulaElContador() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EscalationMetricsObserver observer = new EscalationMetricsObserver(registry);

        observer.onTicketEscalated(ticket(), "SLA vencido");
        observer.onTicketEscalated(ticket(), "Critico sin avance");

        double valor = registry.get("app_business_events_total")
                .tag("event", "ticket_escalated")
                .counter()
                .count();
        assertThat(valor).isEqualTo(2.0);
    }
}
