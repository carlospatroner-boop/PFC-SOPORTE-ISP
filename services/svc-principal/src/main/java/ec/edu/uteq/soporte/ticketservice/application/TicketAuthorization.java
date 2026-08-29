package ec.edu.uteq.soporte.ticketservice.application;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Matriz de autorizacion por rol (roles y permisos definidos en
 * auth-service/PermissionCatalog.java, aqui solo se hacen cumplir), compartida entre los
 * distintos manejadores de comando y la consulta de tickets -- antes del refactor esta
 * logica vivia duplicada como metodos privados dentro de un unico TicketService.
 *
 * <pre>
 * Operacion         CLIENTE                    TECNICO                      ADMIN
 * Crear             si (clientId = propio)     no (403)                     si
 * Listar            solo sus propios tickets   solo los de su zona          sin restriccion
 * Ver por id        solo si es suyo (403)      solo si es de su zona (403)  sin restriccion
 * Cambiar estado     no (403)                  solo si es de su zona (403) sin restriccion
 * Asignar tecnico   no (403)                  solo si es de su zona (403) sin restriccion
 * </pre>
 *
 * Un TECNICO sin zona reconocible (authZone == null) no obtiene acceso amplio -- se trata
 * como "sin zona" (fail-closed), no como "todas las zonas".
 */
@Component
public class TicketAuthorization {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TECNICO = "TECNICO";
    private static final String ROLE_CLIENTE = "CLIENTE";

    public void assertCanView(Ticket ticket, String role, UUID userId, Zone authZone) {
        if (ROLE_ADMIN.equals(role)) {
            return;
        }
        if (ROLE_TECNICO.equals(role)) {
            if (authZone == null || ticket.getZone() != authZone) {
                throw new ForbiddenException("El ticket no pertenece a tu zona");
            }
            return;
        }
        if (ROLE_CLIENTE.equals(role) && ticket.getClientId().equals(userId)) {
            return;
        }
        throw new ForbiddenException("No tienes acceso a este ticket");
    }

    // Usado para updateStatus/assignTechnician: CLIENTE nunca puede, TECNICO solo dentro
    // de su zona.
    public void assertCanManage(Ticket ticket, String role, Zone authZone) {
        if (ROLE_ADMIN.equals(role)) {
            return;
        }
        if (ROLE_TECNICO.equals(role)) {
            if (authZone == null || ticket.getZone() != authZone) {
                throw new ForbiddenException("El ticket no pertenece a tu zona");
            }
            return;
        }
        throw new ForbiddenException("No tienes permiso para modificar tickets");
    }

    public void assertCanCreate(String role) {
        if (ROLE_TECNICO.equals(role)) {
            throw new ForbiddenException("Los tecnicos no pueden crear tickets");
        }
    }
}
