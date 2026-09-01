package ec.edu.uteq.soporte.ticketservice.domain.correlation;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;

import java.util.List;

/**
 * Patron Strategy (Adicion 1 de la Ampliacion del Modulo G, equipo ACC -- ver
 * docs/adr/0008-correl-incidencias.md): decide si un Ticket nuevo se agrupa en una Incidencia
 * abierta existente (misma zona, dentro de la ventana deslizante) o si debe abrir una nueva.
 *
 * <p>Antes de esta Adicion no existia ningun concepto de "Incidencia" en el sistema -- el punto
 * de agrupamiento que la Guia de Reutilizacion asumia que ya existia no estaba, se construyo
 * junto con este patron (ver ADR-0008). La variable de entorno {@code CORREL} (c0/c1/c2)
 * selecciona la implementacion activa en tiempo de arranque -- ver
 * application/correlation/CorrelationService.java.
 */
public interface CorrelationStrategy {

    /**
     * @param ticket el ticket recien creado
     * @param incidenciasAbiertasEnZona candidatas ya existentes en la misma zona, dentro de la
     *                                  ventana deslizante (ver IncidenciaRepository)
     * @return la Incidencia a la que "ticket" debe pertenecer -- existente (si se decide unir)
     *         o una nueva recien construida (todavia sin persistir). Nunca null.
     */
    Incidencia correlacionar(Ticket ticket, List<Incidencia> incidenciasAbiertasEnZona);
}
