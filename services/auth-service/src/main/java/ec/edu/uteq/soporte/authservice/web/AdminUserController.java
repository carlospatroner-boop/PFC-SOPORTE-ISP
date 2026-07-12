package ec.edu.uteq.soporte.authservice.web;

import ec.edu.uteq.soporte.authservice.service.AuthService;
import ec.edu.uteq.soporte.authservice.web.dto.ApiResponse;
import ec.edu.uteq.soporte.authservice.web.dto.CreateUserRequest;
import ec.edu.uteq.soporte.authservice.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Alta de usuarios con rol elegido (CLIENTE/TECNICO/ADMIN) -- a diferencia del
 * registro publico, que siempre fuerza CLIENTE. Requiere un access token con rol
 * ADMIN (ver SecurityConfig + JwtAuthenticationFilter).
 */
@RestController
@RequestMapping("/api/v1/auth/admin")
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.of(authService.createUserAsAdmin(request), "Usuario creado");
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> listUsers() {
        return ApiResponse.of(authService.listUsers(), "OK");
    }
}
