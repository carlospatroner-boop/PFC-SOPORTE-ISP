package ec.edu.uteq.soporte.authservice.web.dto;

import java.time.OffsetDateTime;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresAt) {
}
