package ec.edu.uteq.soporte.authservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador tecnico de Spring Data JPA -- SOLO lo usa RefreshTokenRepositoryAdapter,
 * mismo criterio de encapsulamiento que SpringDataUserRepository.
 */
interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenJpaEntity> findAllByUserIdAndRevokedFalse(UUID userId);

    long countByRevokedFalseAndExpiresAtAfter(OffsetDateTime now);
}
