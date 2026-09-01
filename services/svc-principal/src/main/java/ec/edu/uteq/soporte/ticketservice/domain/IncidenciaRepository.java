package ec.edu.uteq.soporte.ticketservice.domain;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Puerto (patron Repository) del dominio para Incidencia -- mismo criterio que
 * domain/TicketRepository.java: el dominio y la capa de aplicacion no conocen JPA ni SQL. La
 * implementacion real vive en infrastructure/persistence/IncidenciaRepositoryAdapter.java.
 */
public interface IncidenciaRepository {

    /**
     * Candidatas para agrupar un ticket nuevo: incidencias de esa zona abiertas dentro de la
     * ventana deslizante (creadas despues de {@code desde}). Las estrategias c1/c2 consultan
     * esto antes de decidir si un ticket se une a una existente o abre una nueva.
     */
    List<Incidencia> findByZoneAndCreatedAtAfter(Zone zone, OffsetDateTime desde);

    List<Incidencia> findByZone(Zone zone);

    List<Incidencia> findAll();

    Incidencia save(Incidencia incidencia);
}
