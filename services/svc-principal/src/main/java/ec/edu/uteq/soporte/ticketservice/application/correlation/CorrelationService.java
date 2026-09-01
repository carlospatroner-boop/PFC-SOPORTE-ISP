package ec.edu.uteq.soporte.ticketservice.application.correlation;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.IncidenciaRepository;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.correlation.CorrelationStrategy;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Orquesta la correlacion de un ticket nuevo en una Incidencia (Adicion 1 de la Ampliacion del
 * Modulo G, equipo ACC -- ver docs/adr/0008-correl-incidencias.md). Spring inyecta
 * automaticamente un Map con TODOS los beans que implementan CorrelationStrategy, con el
 * nombre del bean como clave -- por eso las 3 implementaciones estan anotadas
 * {@code @Component("c0")}/{@code "c1"}/{@code "c2"}: es exactamente el valor que puede traer la
 * variable de entorno {@code CORREL}.
 *
 * <p>Se resuelve la estrategia activa UNA sola vez al construir este bean (CORREL no cambia en
 * caliente). Cualquier fallo durante la correlacion (incluida la caida del canal de telemetria
 * en modo c2) se atrapa y se registra, pero NUNCA revierte ni bloquea la creacion del ticket --
 * mismo principio que ya aplica el publish de Kafka (ver ADR-0004 y CreateTicketHandler).
 */
@Component
public class CorrelationService {

    private static final Logger LOGGER = Logger.getLogger(CorrelationService.class.getName());

    private final CorrelationStrategy estrategiaActiva;
    private final IncidenciaRepository incidenciaRepository;
    private final long ventanaMinutos;

    public CorrelationService(
            Map<String, CorrelationStrategy> estrategias,
            IncidenciaRepository incidenciaRepository,
            @Value("${correlation.mode:c0}") String correlMode,
            @Value("${correlation.window-minutes:15}") long ventanaMinutos) {
        CorrelationStrategy resuelta = estrategias.get(correlMode);
        if (resuelta == null) {
            throw new IllegalStateException(
                    "CORREL='" + correlMode + "' no coincide con ninguna estrategia registrada "
                            + "(valores validos: " + estrategias.keySet() + ")");
        }
        this.estrategiaActiva = resuelta;
        this.incidenciaRepository = incidenciaRepository;
        this.ventanaMinutos = ventanaMinutos;
        LOGGER.info("CorrelationService activo con CORREL=" + correlMode + ", ventana=" + ventanaMinutos + "min");
    }

    public void correlacionar(Ticket ticket) {
        try {
            OffsetDateTime desde = ticket.getCreatedAt().minusMinutes(ventanaMinutos);
            List<Incidencia> candidatas = incidenciaRepository.findByZoneAndCreatedAtAfter(ticket.getZone(), desde);
            Incidencia resultado = estrategiaActiva.correlacionar(ticket, candidatas);
            incidenciaRepository.save(resultado);
        } catch (Exception e) {
            // Nunca debe tumbar ni revertir la creacion del ticket -- ver ADR-0004.
            LOGGER.log(Level.WARNING, "No se pudo correlacionar el ticket " + ticket.getId(), e);
        }
    }
}
