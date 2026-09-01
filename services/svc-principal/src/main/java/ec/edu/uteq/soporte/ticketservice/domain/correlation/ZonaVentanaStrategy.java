package ec.edu.uteq.soporte.ticketservice.domain.correlation;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * CORREL=c1: agrupa por zona y ventana deslizante. Si ya existe una Incidencia abierta en la
 * misma zona dentro de la ventana (candidatas ya filtradas por IncidenciaRepository antes de
 * llegar aqui), el ticket se une a la mas reciente; si no hay ninguna, abre una nueva.
 *
 * <p>Deliberadamente ciega a la telemetria (a diferencia de c2): agrupa por la sola coincidencia
 * de zona+tiempo, sin corroborar que realmente este pasando algo ahi. Esto es a proposito --
 * es exactamente lo que el Escenario 4 (dos averias simultaneas) del protocolo experimental
 * pone a prueba: agrupar solo por zona+ventana puede fundir dos averias distintas en una.
 */
@Component("c1")
public class ZonaVentanaStrategy implements CorrelationStrategy {

    @Override
    public Incidencia correlacionar(Ticket ticket, List<Incidencia> incidenciasAbiertasEnZona) {
        return incidenciasAbiertasEnZona.stream()
                .max(Comparator.comparing(Incidencia::getCreatedAt))
                .map(existente -> unir(existente, ticket))
                .orElseGet(() -> abrirNueva(ticket));
    }

    private Incidencia unir(Incidencia existente, Ticket ticket) {
        existente.getTicketIds().add(ticket.getId());
        return existente;
    }

    private Incidencia abrirNueva(Ticket ticket) {
        return Incidencia.builder()
                .zone(ticket.getZone())
                .createdAt(ticket.getCreatedAt())
                .correlMode("c1")
                .ticketIds(new HashSet<>(List.of(ticket.getId())))
                .build();
    }
}
