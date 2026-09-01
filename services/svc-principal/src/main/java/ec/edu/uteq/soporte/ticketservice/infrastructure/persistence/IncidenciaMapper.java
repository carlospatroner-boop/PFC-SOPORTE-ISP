package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/** Traduce entre el modelo de dominio puro (Incidencia) y su mapeo JPA (IncidenciaJpaEntity). */
@Component
public class IncidenciaMapper {

    public Incidencia toDomain(IncidenciaJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Incidencia.builder()
                .id(entity.getId())
                .zone(entity.getZone())
                .createdAt(entity.getCreatedAt())
                .correlMode(entity.getCorrelMode())
                .ticketIds(new HashSet<>(entity.getTicketIds()))
                .build();
    }

    public IncidenciaJpaEntity toEntity(Incidencia incidencia) {
        if (incidencia == null) {
            return null;
        }
        return IncidenciaJpaEntity.builder()
                .id(incidencia.getId())
                .zone(incidencia.getZone())
                .createdAt(incidencia.getCreatedAt())
                .correlMode(incidencia.getCorrelMode())
                .ticketIds(new HashSet<>(incidencia.getTicketIds()))
                .build();
    }
}
