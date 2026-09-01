package ec.edu.uteq.soporte.ticketservice.domain.correlation;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CORREL=c2: mismo criterio de zona+ventana que c1, mas una consulta real al canal de
 * telemetria de PE-U1 (ver {@link TelemetryQueryPort}) antes de agrupar. Solo une el ticket a
 * una Incidencia existente (o abre una nueva compartida) si la telemetria corrobora que algo
 * esta pasando en esa zona -- si no hay evidencia (o el canal esta caido), el ticket queda
 * aislado en su propia Incidencia, igual que haria c0.
 *
 * <p>Esto NO esta disenado para resolver el Escenario 4 del protocolo (dos averias
 * simultaneas en la misma zona a la vez) -- la telemetria confirma que "algo pasa en la zona",
 * no distingue CUAL averia. Cuando dos averias reales golpean la misma zona en la misma
 * ventana, c2 puede fundirlas igual que c1. Eso es intencional: el punto del Escenario 4 es
 * revelar ese error, no que esta estrategia lo evite (ver docs/adr/0008-correl-incidencias.md).
 */
@Component("c2")
public class ZonaVentanaTelemetriaStrategy implements CorrelationStrategy {

    private final TelemetryQueryPort telemetryQueryPort;
    private final long ventanaSegundos;

    public ZonaVentanaTelemetriaStrategy(
            TelemetryQueryPort telemetryQueryPort,
            @Value("${correlation.window-minutes:15}") long ventanaMinutos) {
        this.telemetryQueryPort = telemetryQueryPort;
        this.ventanaSegundos = ventanaMinutos * 60;
    }

    @Override
    public Incidencia correlacionar(Ticket ticket, List<Incidencia> incidenciasAbiertasEnZona) {
        boolean hayEvidencia = telemetryQueryPort.hayEvidenciaDeAveria(ticket.getZone(), ventanaSegundos);
        if (!hayEvidencia) {
            return abrirNueva(ticket);
        }
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
                .correlMode("c2")
                .ticketIds(new HashSet<>(List.of(ticket.getId())))
                .build();
    }
}
