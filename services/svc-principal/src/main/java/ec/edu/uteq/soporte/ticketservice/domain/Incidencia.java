package ec.edu.uteq.soporte.ticketservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad de dominio Incidencia (Adicion 1 de la Ampliacion del Modulo G, equipo ACC):
 * agrupacion de uno o mas Ticket que la estrategia de correlacion activa (ver
 * domain/correlation/CorrelationStrategy.java) considero parte de la misma averia. Igual que
 * Ticket, deliberadamente SIN anotaciones JPA -- el mapeo real vive en
 * infrastructure/persistence/IncidenciaJpaEntity.java + IncidenciaMapper.java.
 *
 * <p>Con la estrategia c0 (linea base, sin correlacion) cada Ticket abre su propia Incidencia
 * de un solo elemento -- "una incidencia" no implica necesariamente "una averia real": es lo
 * que el sistema decidio agrupar, y es exactamente lo que el experimento de la seccion 5 de la
 * Guia de Reutilizacion mide contra la verdad de campo real (ver
 * experimentos/inyector_averias.py).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incidencia {

    private UUID id;
    private Zone zone;
    private OffsetDateTime createdAt;
    /** c0 | c1 | c2 -- con que estrategia se abrio, para poder auditar/comparar despues. */
    private String correlMode;
    @Builder.Default
    private Set<UUID> ticketIds = new HashSet<>();
}
