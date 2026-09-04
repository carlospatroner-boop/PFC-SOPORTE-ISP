package ec.edu.uteq.soporte.authservice.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto (patron Repository) del dominio para RefreshToken -- mismo criterio que
 * UserRepository. La implementacion real vive en
 * infrastructure/persistence/RefreshTokenRepositoryAdapter.java.
 */
public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId);

    // Alimenta la metrica "app_active_sessions" (Modulo F, item 3): una sesion se
    // considera activa mientras su refresh token no este revocado ni vencido -- es la
    // unica nocion de "sesion" que existe en un sistema de access tokens sin estado.
    long countByRevokedFalseAndExpiresAtAfter(OffsetDateTime now);

    RefreshToken save(RefreshToken refreshToken);

    List<RefreshToken> saveAll(List<RefreshToken> tokens);
}
