package ec.edu.uteq.soporte.authservice.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registro publico de auto-servicio. Siempre crea un usuario CLIENTE -- no incluye
 * un campo `role` a proposito, para que nadie pueda auto-asignarse TECNICO/ADMIN
 * (ver AuthService.register / CreateUserRequest para el alta administrativa).
 */
public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, message = "la contrasena debe tener al menos 8 caracteres") String password,
        @NotBlank String fullName) {
}
