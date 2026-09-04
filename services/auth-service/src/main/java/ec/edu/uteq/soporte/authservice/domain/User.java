package ec.edu.uteq.soporte.authservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad de dominio Usuario -- deliberadamente SIN anotaciones JPA (mismo criterio que
 * Ticket en ticket-service, ver ADR-0005): el dominio y la capa de aplicacion no conocen
 * Hibernate ni la tabla `users` de la base `auth_db`. El mapeo real vive en
 * infrastructure/persistence/UserJpaEntity.java + UserMapper.java.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private UUID id;
    private String email;
    private String passwordHash;
    private String fullName;
    private Role role;

    // Solo se llena para TECNICO. Cadena plana (no un enum propio de auth-service)
    // porque este servicio no necesita entender zonas, solo guardarla y viajarla
    // como claim del JWT -- debe coincidir exactamente con el enum Zone de
    // ticket-service (QUEVEDO_CENTRO | QUEVEDO_NORTE | QUEVEDO_SUR).
    private String zone;

    private boolean active;
    private OffsetDateTime createdAt;
}
