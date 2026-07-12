package ec.edu.uteq.soporte.authservice.web.dto;

import ec.edu.uteq.soporte.authservice.domain.User;

import java.time.OffsetDateTime;

public record UserResponse(
        String id,
        String email,
        String fullName,
        String role,
        String zone,
        boolean active,
        OffsetDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getZone(),
                user.isActive(),
                user.getCreatedAt());
    }
}
