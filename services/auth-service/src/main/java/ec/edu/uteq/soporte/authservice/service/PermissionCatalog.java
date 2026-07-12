package ec.edu.uteq.soporte.authservice.service;

import ec.edu.uteq.soporte.authservice.domain.Role;

import java.util.List;
import java.util.Map;

/**
 * Mapa estatico rol -> permisos, embebido como claim en el access token para que
 * los servicios consumidores (ticket-service, futuro API Gateway) puedan autorizar
 * sin tener que volver a consultar auth-service en cada request.
 */
public final class PermissionCatalog {

    private static final Map<Role, List<String>> PERMISSIONS = Map.of(
            Role.CLIENTE, List.of("ticket:create", "ticket:read:own"),
            Role.TECNICO, List.of("ticket:read:zone", "ticket:update:status", "ticket:assign"),
            Role.ADMIN, List.of("*")
    );

    private PermissionCatalog() {
    }

    public static List<String> permissionsFor(Role role) {
        return PERMISSIONS.getOrDefault(role, List.of());
    }
}
