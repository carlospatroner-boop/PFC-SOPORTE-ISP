package ec.edu.uteq.soporte.authservice.event;

/**
 * Payload publicado en "technician.created" cuando se da de alta un usuario con rol
 * TECNICO (ver AuthService.createUserAsAdmin). Consumido por ticket-service para
 * sincronizar su propia tabla `technicians` -- ver TicketAssignmentException y
 * tickets_technician_id_fkey en db-cluster/scripts/init_db.sql: assignTechnician exige que
 * el id exista ahi, no en auth_db.users.
 */
public record TechnicianCreatedEvent(String technicianId, String fullName, String zone) {
}
