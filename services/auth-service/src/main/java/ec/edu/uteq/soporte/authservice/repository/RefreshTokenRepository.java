package ec.edu.uteq.soporte.authservice.repository;

import ec.edu.uteq.soporte.authservice.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId);

    // Alimenta la metrica "app_active_sessions" (Modulo F, item 3): una sesion se
    // considera activa mientras su refresh token no este revocado ni vencido -- es la
    // unica nocion de "sesion" que existe en un sistema de access tokens sin estado.
    long countByRevokedFalseAndExpiresAtAfter(OffsetDateTime now);
}
