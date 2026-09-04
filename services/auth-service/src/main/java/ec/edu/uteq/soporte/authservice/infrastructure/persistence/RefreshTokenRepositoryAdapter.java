package ec.edu.uteq.soporte.authservice.infrastructure.persistence;

import ec.edu.uteq.soporte.authservice.domain.RefreshToken;
import ec.edu.uteq.soporte.authservice.domain.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador (patron Repository) que implementa el puerto de dominio
 * RefreshTokenRepository delegando en Spring Data JPA -- mismo criterio que
 * UserRepositoryAdapter.
 */
@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository jpaRepository;
    private final RefreshTokenMapper mapper;

    public RefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository jpaRepository, RefreshTokenMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId) {
        return jpaRepository.findAllByUserIdAndRevokedFalse(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByRevokedFalseAndExpiresAtAfter(OffsetDateTime now) {
        return jpaRepository.countByRevokedFalseAndExpiresAtAfter(now);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity saved = jpaRepository.save(mapper.toEntity(refreshToken));
        return mapper.toDomain(saved);
    }

    @Override
    public List<RefreshToken> saveAll(List<RefreshToken> tokens) {
        List<RefreshTokenJpaEntity> entities = tokens.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }
}
