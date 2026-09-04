package ec.edu.uteq.soporte.authservice.presentation.dto;

import java.time.OffsetDateTime;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresAt) {
}
