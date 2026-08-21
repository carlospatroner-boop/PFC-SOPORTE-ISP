package ec.edu.uteq.soporte.ticketservice.domain.event;

/**
 * Vista de ticket-service del payload que publica auth-service en "technician.created"
 * (ver TechnicianEventPublisher alla). Es el otro lado de la Saga por coreografia que
 * sincroniza la tabla local `technicians` -- sin esto, cualquier TECNICO dado de alta desde
 * el panel de Admin nunca aparece aqui y assignTechnician falla con una violacion de la FK
 * tickets_technician_id_fkey (bug real encontrado probando la consola web con un tecnico
 * nuevo).
 */
public record TechnicianCreatedEvent(String technicianId, String fullName, String zone) {
}
