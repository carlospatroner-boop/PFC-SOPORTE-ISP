package ec.edu.uteq.soporte.ticketservice.infrastructure.metrics;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.EscalationObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Observador concreto: registra cada escalado como un evento de negocio en Prometheus.
 * "app_business_events_total" es una de las cuatro metricas nuevas que exige el Modulo F
 * (D6, observabilidad) de la guia de Entrega 4 -- se adelanta aqui porque el escalado es,
 * precisamente, el primer evento de negocio real que el sistema dispara automaticamente.
 */
@Component
public class EscalationMetricsObserver implements EscalationObserver {

    private final Counter escalatedTicketsCounter;

    public EscalationMetricsObserver(MeterRegistry registry) {
        this.escalatedTicketsCounter = Counter.builder("app_business_events_total")
                .description("Eventos de negocio disparados por el sistema, con la etiqueta 'event'")
                .tag("event", "ticket_escalated")
                .register(registry);
    }

    @Override
    public void onTicketEscalated(Ticket ticket, String motivo) {
        escalatedTicketsCounter.increment();
    }
}
