package ec.edu.uteq.soporte.ticketservice.domain.correlation;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * CORREL=c0 (linea base): sin correlacion. Cada ticket abre su propia Incidencia de un solo
 * elemento -- ignora por completo las candidatas de la misma zona. Es el punto de comparacion
 * contra el que c1/c2 se miden en el experimento (ver experimentos/analizar_correlacion.py).
 */
@Component("c0")
public class SinCorrelacionStrategy implements CorrelationStrategy {

    @Override
    public Incidencia correlacionar(Ticket ticket, List<Incidencia> incidenciasAbiertasEnZona) {
        return Incidencia.builder()
                .zone(ticket.getZone())
                .createdAt(ticket.getCreatedAt())
                .correlMode("c0")
                .ticketIds(Set.of(ticket.getId()))
                .build();
    }
}
