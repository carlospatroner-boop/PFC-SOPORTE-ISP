package ec.edu.uteq.soporte.ticketservice.domain.policy;

import ec.edu.uteq.soporte.ticketservice.domain.Priority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Estrategia usada una vez que ai-service clasifico el ticket y se conoce su prioridad real
 * (ver infrastructure/messaging/TicketClassificationListener). Mismos plazos que se usaban
 * hardcodeados antes del refactor, ahora nombrados y sustituibles.
 */
@Component
public class ClassifiedSlaPolicy implements SlaPolicy {

    private static final Map<Priority, Duration> SLA_POR_PRIORIDAD = Map.of(
            Priority.CRITICO, Duration.ofHours(4),
            Priority.ALTO, Duration.ofHours(12),
            Priority.MEDIO, Duration.ofHours(24),
            Priority.BAJO, Duration.ofHours(48)
    );

    @Override
    public Duration slaFor(Priority priority) {
        return SLA_POR_PRIORIDAD.getOrDefault(priority, Duration.ofHours(24));
    }
}
