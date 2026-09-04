package ec.edu.uteq.soporte.authservice.infrastructure.persistence;

import ec.edu.uteq.soporte.authservice.domain.User;
import org.springframework.stereotype.Component;

/** Traduce entre el modelo de dominio puro (User) y su mapeo JPA (UserJpaEntity). */
@Component
public class UserMapper {

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .fullName(entity.getFullName())
                .role(entity.getRole())
                .zone(entity.getZone())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public UserJpaEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserJpaEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .fullName(user.getFullName())
                .role(user.getRole())
                .zone(user.getZone())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
