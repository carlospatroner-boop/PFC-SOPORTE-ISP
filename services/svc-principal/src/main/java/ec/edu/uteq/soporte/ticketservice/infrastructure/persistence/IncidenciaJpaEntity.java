package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
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
 * Mapeo JPA real de Incidencia a las tablas `incidencias` + `incidencia_tickets` de
 * CockroachDB (ver db-cluster/scripts/init_db.sql). Tabla chica y no particionada, a
 * diferencia de `tickets` -- es una agrupacion, no el registro de negocio principal (mismo
 * criterio que la tabla `technicians`). Los ticketIds se mapean con @ElementCollection, sin
 * necesidad de una entidad JPA propia para la tabla de union.
 */
@Entity
@Table(name = "incidencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidenciaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false)
    private Zone zone;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "correl_mode", nullable = false)
    private String correlMode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "incidencia_tickets", joinColumns = @JoinColumn(name = "incidencia_id"))
    @Column(name = "ticket_id")
    @Builder.Default
    private Set<UUID> ticketIds = new HashSet<>();
}
