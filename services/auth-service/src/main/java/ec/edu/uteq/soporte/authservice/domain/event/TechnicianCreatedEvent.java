package ec.edu.uteq.soporte.authservice.domain.event;

/**
 * Payload publicado en "technician.created" cuando se da de alta un usuario con rol
 * TECNICO (ver application/AuthService.createUserAsAdmin). Consumido por ticket-service
 * para sincronizar su propia tabla `technicians` -- ver TicketAssignmentException y
 * tickets_technician_id_fkey en services/svc-principal/src/main/resources/db/migration/V1__init_ticket_schema.sql: assignTechnician exige
 * que el id exista ahi, no en auth_db.users.
 */
public record TechnicianCreatedEvent(String technicianId, String fullName, String zone) {
}
