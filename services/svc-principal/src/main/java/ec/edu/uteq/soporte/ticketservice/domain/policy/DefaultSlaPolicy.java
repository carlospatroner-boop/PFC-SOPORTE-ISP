package ec.edu.uteq.soporte.ticketservice.domain.policy;

import ec.edu.uteq.soporte.ticketservice.domain.Priority;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Estrategia usada al crear un ticket, cuando ai-service todavia no asigno una prioridad real
 * (ver TicketFactory.crearNuevo) -- SLA plano de 24h independientemente de la categoria.
 */
@Component
public class DefaultSlaPolicy implements SlaPolicy {

    private static final Duration SLA_POR_DEFECTO = Duration.ofHours(24);

    @Override
    public Duration slaFor(Priority priority) {
        return SLA_POR_DEFECTO;
    }
}
