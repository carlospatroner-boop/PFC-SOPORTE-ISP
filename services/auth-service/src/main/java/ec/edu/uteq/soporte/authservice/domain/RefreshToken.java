package ec.edu.uteq.soporte.authservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Refresh token opaco (no es un JWT) -- deliberadamente SIN anotaciones JPA (mismo
 * criterio que User, ver ADR-0005). Solo se persiste el hash SHA-256 del valor crudo
 * (`tokenHash`), nunca el valor original. La fila mapeada en
 * infrastructure/persistence/RefreshTokenJpaEntity.java es la unica fuente de verdad
 * para saber si un refresh token sigue siendo valido (a diferencia del access token,
 * que es stateless y expira solo).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private OffsetDateTime issuedAt;
    private OffsetDateTime expiresAt;
    private boolean revoked;
    private UUID replacedBy;
}
