package ec.edu.uteq.soporte.authservice.web;

import ec.edu.uteq.soporte.authservice.exception.InvalidTokenException;
import ec.edu.uteq.soporte.authservice.service.AuthService;
import ec.edu.uteq.soporte.authservice.web.dto.ApiResponse;
import ec.edu.uteq.soporte.authservice.web.dto.AuthResponse;
import ec.edu.uteq.soporte.authservice.web.dto.LoginRequest;
import ec.edu.uteq.soporte.authservice.web.dto.LogoutRequest;
import ec.edu.uteq.soporte.authservice.web.dto.RefreshRequest;
import ec.edu.uteq.soporte.authservice.web.dto.RegisterRequest;
import ec.edu.uteq.soporte.authservice.web.dto.UserResponse;
import ec.edu.uteq.soporte.authservice.web.dto.ValidateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.of(authService.register(request), "Usuario registrado");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(authService.login(request.email(), request.password()), "Sesion iniciada");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.of(authService.refresh(request.refreshToken()), "Token renovado");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.of(null, "Sesion cerrada");
    }

    // "/validate" queda permitAll() en SecurityConfig: el parseo del token ocurre aqui
    // mismo (no via SecurityContext) para que un token invalido/expirado devuelva un
    // 401 con el envoltorio ApiResponse en vez del handler generico de Spring Security.
    @GetMapping("/validate")
    public ApiResponse<ValidateResponse> validate(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("Encabezado Authorization ausente o mal formado");
        }
        String token = authorizationHeader.substring("Bearer ".length());
        return ApiResponse.of(authService.validate(token), "Token valido");
    }
}
