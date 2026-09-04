package ec.edu.uteq.soporte.authservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {
}
