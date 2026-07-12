package ec.edu.uteq.soporte.authservice.service;

import ec.edu.uteq.soporte.authservice.domain.RefreshToken;
import ec.edu.uteq.soporte.authservice.domain.Role;
import ec.edu.uteq.soporte.authservice.domain.User;
import ec.edu.uteq.soporte.authservice.exception.DuplicateEmailException;
import ec.edu.uteq.soporte.authservice.exception.InvalidCredentialsException;
import ec.edu.uteq.soporte.authservice.exception.InvalidRequestException;
import ec.edu.uteq.soporte.authservice.exception.InvalidTokenException;
import ec.edu.uteq.soporte.authservice.exception.TokenReuseDetectedException;
import ec.edu.uteq.soporte.authservice.exception.UserNotFoundException;
import ec.edu.uteq.soporte.authservice.repository.RefreshTokenRepository;
import ec.edu.uteq.soporte.authservice.repository.UserRepository;
import ec.edu.uteq.soporte.authservice.web.dto.AuthResponse;
import ec.edu.uteq.soporte.authservice.web.dto.CreateUserRequest;
import ec.edu.uteq.soporte.authservice.web.dto.RegisterRequest;
import ec.edu.uteq.soporte.authservice.web.dto.UserResponse;
import ec.edu.uteq.soporte.authservice.web.dto.ValidateResponse;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    // Debe coincidir exactamente con el enum Zone de ticket-service.
    private static final Set<String> VALID_ZONES = Set.of("QUEVEDO_CENTRO", "QUEVEDO_NORTE", "QUEVEDO_SUR");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        return UserResponse.from(
                createUser(request.email(), request.password(), request.fullName(), Role.CLIENTE, null));
    }

    @Transactional
    public UserResponse createUserAsAdmin(CreateUserRequest request) {
        validateZoneForRole(request.role(), request.zone());
        return UserResponse.from(createUser(
                request.email(), request.password(), request.fullName(), request.role(), request.zone()));
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    // TECNICO necesita una zona valida (para poder filtrar "tickets de mi zona" en
    // ticket-service via el claim del JWT); los demas roles no deben traer una,
    // para no dejar datos ambiguos/inconsistentes en la fila del usuario.
    private void validateZoneForRole(Role role, String zone) {
        if (role == Role.TECNICO) {
            if (zone == null || zone.isBlank() || !VALID_ZONES.contains(zone)) {
                throw new InvalidRequestException(
                        "TECNICO requiere una zona valida: " + String.join(", ", VALID_ZONES));
            }
        } else if (zone != null && !zone.isBlank()) {
            throw new InvalidRequestException("Solo TECNICO puede tener una zona asignada");
        }
    }

    private User createUser(String email, String rawPassword, String fullName, Role role, String zone) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("El correo ya esta registrado: " + email);
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .role(role)
                .zone(zone)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales invalidas"));
        if (!user.isActive()) {
            throw new InvalidCredentialsException("La cuenta esta inactiva");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciales invalidas");
        }
        return issueTokenPair(user);
    }

    // noRollbackFor: si se detecta reuso, SI se debe conservar la revocacion masiva
    // hecha en revokeAllForUser() aunque el metodo termine lanzando una excepcion --
    // de lo contrario Spring revertiria toda la transaccion (comportamiento por
    // defecto ante un RuntimeException) y la revocacion nunca llegaria a la base.
    @Transactional(noRollbackFor = TokenReuseDetectedException.class)
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token invalido"));

        if (existing.isRevoked()) {
            // El token ya fue rotado/revocado antes: esto es reuso, tratar como robo.
            revokeAllForUser(existing.getUserId());
            throw new TokenReuseDetectedException(
                    "Se detecto reuso de un refresh token ya revocado; se cerraron todas las sesiones del usuario");
        }
        if (existing.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException("Refresh token expirado");
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        IssuedPair issued = issueTokenPairInternal(user);

        existing.setRevoked(true);
        existing.setReplacedBy(issued.refreshTokenId());
        refreshTokenRepository.save(existing);

        return issued.response();
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token invalido"));
        if (!existing.isRevoked()) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
        }
    }

    public ValidateResponse validate(String bearerToken) {
        Claims claims = jwtService.parseAndValidate(bearerToken);
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) claims.get("permissions", List.class);
        return new ValidateResponse(
                claims.getSubject(),
                claims.get("email", String.class),
                claims.get("role", String.class),
                claims.get("zone", String.class),
                permissions,
                OffsetDateTime.ofInstant(claims.getExpiration().toInstant(), java.time.ZoneOffset.UTC));
    }

    private AuthResponse issueTokenPair(User user) {
        return issueTokenPairInternal(user).response();
    }

    private IssuedPair issueTokenPairInternal(User user) {
        JwtService.IssuedAccessToken accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = generateOpaqueToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashToken(rawRefreshToken))
                .issuedAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plus(jwtService.refreshTokenTtl()))
                .revoked(false)
                .build();
        refreshToken = refreshTokenRepository.save(refreshToken);

        AuthResponse response = new AuthResponse(accessToken.token(), rawRefreshToken, accessToken.expiresAt());
        return new IssuedPair(response, refreshToken.getId());
    }

    private record IssuedPair(AuthResponse response, UUID refreshTokenId) {
    }

    private void revokeAllForUser(UUID userId) {
        List<RefreshToken> active = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        active.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(active);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
