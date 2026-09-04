package ec.edu.uteq.soporte.authservice.infrastructure.persistence;

import ec.edu.uteq.soporte.authservice.domain.RefreshToken;
import org.springframework.stereotype.Component;

/** Traduce entre el modelo de dominio puro (RefreshToken) y su mapeo JPA (RefreshTokenJpaEntity). */
@Component
public class RefreshTokenMapper {

    public RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return RefreshToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .issuedAt(entity.getIssuedAt())
                .expiresAt(entity.getExpiresAt())
                .revoked(entity.isRevoked())
                .replacedBy(entity.getReplacedBy())
                .build();
    }

    public RefreshTokenJpaEntity toEntity(RefreshToken token) {
        if (token == null) {
            return null;
        }
        return RefreshTokenJpaEntity.builder()
                .id(token.getId())
                .userId(token.getUserId())
                .tokenHash(token.getTokenHash())
                .issuedAt(token.getIssuedAt())
                .expiresAt(token.getExpiresAt())
                .revoked(token.isRevoked())
                .replacedBy(token.getReplacedBy())
                .build();
    }
}
