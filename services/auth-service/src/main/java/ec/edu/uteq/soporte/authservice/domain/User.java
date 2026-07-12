package ec.edu.uteq.soporte.authservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad de usuario, mapeada a la tabla `users` de la base `auth_db`
 * (ver db-cluster/scripts/init_auth_db.sql).
 *
 * A diferencia de `Ticket` en ticket-service, el id usa
 * {@code GenerationType.UUID}: Hibernate lo genera antes del insert sin que el
 * campo se toque manualmente en el codigo de aplicacion, de modo que Spring Data
 * detecta correctamente la entidad como "nueva" (id null en el momento de save())
 * y usa persist() en vez de merge() (evita un SELECT extra innecesario).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    // Solo se llena para TECNICO. Cadena plana (no un enum propio de auth-service)
    // porque este servicio no necesita entender zonas, solo guardarla y viajarla
    // como claim del JWT -- debe coincidir exactamente con el enum Zone de
    // ticket-service (QUEVEDO_CENTRO | QUEVEDO_NORTE | QUEVEDO_SUR).
    @Column(name = "zone")
    private String zone;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
