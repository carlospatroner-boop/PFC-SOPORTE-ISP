package ec.edu.uteq.soporte.authservice.domain;

import java.util.List;
import java.util.Map;

/**
 * Mapa estatico rol -> permisos, embebido como claim en el access token para que
 * los servicios consumidores (ticket-service, api-gateway) puedan autorizar sin tener
 * que volver a consultar auth-service en cada request. Regla de negocio pura -- cero
 * dependencias de framework, mismo criterio que domain/policy/SlaPolicy.java en
 * ticket-service -- por eso vive en domain y no en infrastructure/security junto a
 * JwtService, que solo la consume.
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
