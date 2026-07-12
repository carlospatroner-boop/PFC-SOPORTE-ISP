package ec.edu.uteq.soporte.authservice.web.dto;

import ec.edu.uteq.soporte.authservice.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta administrativa de usuarios (solo accesible con un token ADMIN). A diferencia
 * de RegisterRequest, aqui si se puede elegir el rol -- incluyendo TECNICO/ADMIN.
 *
 * "zone" es obligatoria si role=TECNICO (y prohibida para los demas roles) -- la
 * validacion es condicional al rol, por eso vive en AuthService y no en una
 * anotacion de Bean Validation aqui.
 */
public record CreateUserRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, message = "la contrasena debe tener al menos 8 caracteres") String password,
        @NotBlank String fullName,
        @NotNull Role role,
        String zone) {
}
