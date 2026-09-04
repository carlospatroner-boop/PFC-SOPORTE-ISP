package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba IncidenciaMapper (mismo criterio que TicketMapper, ya probado): traduce en ambas
 * direcciones sin perder datos, y ambas direcciones devuelven null ante null en vez de lanzar
 * NullPointerException (relevante porque IncidenciaRepositoryAdapter llama toDomain sobre el
 * resultado de un findById que puede no existir).
 */
class IncidenciaMapperTest {

    private final IncidenciaMapper mapper = new IncidenciaMapper();

    @Test
    void toDomain_traduceTodosLosCamposDesdeLaEntidadJpa() {
        UUID id = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        Set<UUID> ticketIds = new HashSet<>(Set.of(UUID.randomUUID(), UUID.randomUUID()));
        IncidenciaJpaEntity entity = IncidenciaJpaEntity.builder()
                .id(id)
                .zone(Zone.QUEVEDO_SUR)
                .createdAt(createdAt)
                .correlMode("c2")
                .ticketIds(ticketIds)
                .build();

        Incidencia resultado = mapper.toDomain(entity);

        assertThat(resultado.getId()).isEqualTo(id);
        assertThat(resultado.getZone()).isEqualTo(Zone.QUEVEDO_SUR);
        assertThat(resultado.getCreatedAt()).isEqualTo(createdAt);
        assertThat(resultado.getCorrelMode()).isEqualTo("c2");
        assertThat(resultado.getTicketIds()).isEqualTo(ticketIds);
    }

    @Test
    void toDomain_devuelveNullAnteEntidadNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_traduceTodosLosCamposDesdeElDominio() {
        UUID id = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        Set<UUID> ticketIds = new HashSet<>(Set.of(UUID.randomUUID()));
        Incidencia incidencia = Incidencia.builder()
                .id(id)
                .zone(Zone.QUEVEDO_NORTE)
                .createdAt(createdAt)
                .correlMode("c1")
                .ticketIds(ticketIds)
                .build();

        IncidenciaJpaEntity resultado = mapper.toEntity(incidencia);

        assertThat(resultado.getId()).isEqualTo(id);
        assertThat(resultado.getZone()).isEqualTo(Zone.QUEVEDO_NORTE);
        assertThat(resultado.getCreatedAt()).isEqualTo(createdAt);
        assertThat(resultado.getCorrelMode()).isEqualTo("c1");
        assertThat(resultado.getTicketIds()).isEqualTo(ticketIds);
    }

    @Test
    void toEntity_devuelveNullAnteDominioNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void ida_y_vuelta_preservaTodosLosCampos() {
        Incidencia original = Incidencia.builder()
                .id(UUID.randomUUID())
                .zone(Zone.QUEVEDO_CENTRO)
                .createdAt(OffsetDateTime.now())
                .correlMode("c0")
                .ticketIds(new HashSet<>(Set.of(UUID.randomUUID())))
                .build();

        Incidencia resultado = mapper.toDomain(mapper.toEntity(original));

        // Incidencia no sobreescribe equals()/hashCode() (solo @Getter/@Setter/@Builder), asi
        // que se compara campo a campo por reflexion en vez de con isEqualTo directo.
        assertThat(resultado).usingRecursiveComparison().isEqualTo(original);
    }
}
