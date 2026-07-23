package ec.edu.uteq.soporte.authservice.service;

import ec.edu.uteq.soporte.authservice.domain.RefreshToken;
import ec.edu.uteq.soporte.authservice.domain.Role;
import ec.edu.uteq.soporte.authservice.domain.User;
import ec.edu.uteq.soporte.authservice.exception.InvalidCredentialsException;
import ec.edu.uteq.soporte.authservice.exception.InvalidRequestException;
import ec.edu.uteq.soporte.authservice.exception.TokenReuseDetectedException;
import ec.edu.uteq.soporte.authservice.repository.RefreshTokenRepository;
import ec.edu.uteq.soporte.authservice.repository.UserRepository;
import ec.edu.uteq.soporte.authservice.web.dto.AuthResponse;
import ec.edu.uteq.soporte.authservice.web.dto.CreateUserRequest;
import ec.edu.uteq.soporte.authservice.web.dto.RegisterRequest;
import ec.edu.uteq.soporte.authservice.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias puras (sin contexto de Spring ni base de datos real), igual que
 * TicketServiceTest en ticket-service: repositorios y JwtService mockeados,
 * BCryptPasswordEncoder real (es barato y no vale la pena mockearlo).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, jwtService, passwordEncoder);

        // Simulan lo que Hibernate hace de verdad al persistir (asignar el id generado);
        // lenient() porque no todos los tests ejercitan ambos repositorios.
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        lenient().when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID());
            }
            return token;
        });
    }

    @Test
    void registerHashesPasswordAndForcesClienteRole() {
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);

        UserResponse response = authService.register(new RegisterRequest("nuevo@test.com", "Passw0rd!", "Nuevo Usuario"));

        assertThat(response.role()).isEqualTo(Role.CLIENTE.name());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("Passw0rd!");
        assertThat(passwordEncoder.matches("Passw0rd!", captor.getValue().getPasswordHash())).isTrue();
        assertThat(captor.getValue().getRole()).isEqualTo(Role.CLIENTE);
    }

    @Test
    void listUsersReturnsEveryUserMappedToUserResponse() {
        User cliente = activeUser("cliente@test.com", "Passw0rd!");
        User tecnico = User.builder()
                .id(UUID.randomUUID())
                .email("tec@test.com")
                .role(Role.TECNICO)
                .zone("QUEVEDO_NORTE")
                .fullName("Tecnico Test")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();
        when(userRepository.findAll()).thenReturn(List.of(cliente, tecnico));

        List<UserResponse> users = authService.listUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserResponse::email).containsExactlyInAnyOrder("cliente@test.com", "tec@test.com");
        assertThat(users).filteredOn(u -> u.role().equals("TECNICO")).extracting(UserResponse::zone)
                .containsExactly("QUEVEDO_NORTE");
    }

    @Test
    void createUserAsAdminRequiresValidZoneForTecnico() {
        when(userRepository.existsByEmail("tec@test.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.createUserAsAdmin(
                new CreateUserRequest("tec@test.com", "Passw0rd!", "Tecnico Uno", Role.TECNICO, null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> authService.createUserAsAdmin(
                new CreateUserRequest("tec@test.com", "Passw0rd!", "Tecnico Uno", Role.TECNICO, "ZONA_INVENTADA")))
                .isInstanceOf(InvalidRequestException.class);

        UserResponse response = authService.createUserAsAdmin(
                new CreateUserRequest("tec@test.com", "Passw0rd!", "Tecnico Uno", Role.TECNICO, "QUEVEDO_NORTE"));
        assertThat(response.zone()).isEqualTo("QUEVEDO_NORTE");
    }

    @Test
    void createUserAsAdminRejectsZoneForNonTecnicoRoles() {
        assertThatThrownBy(() -> authService.createUserAsAdmin(
                new CreateUserRequest("otro@test.com", "Passw0rd!", "Otro", Role.CLIENTE, "QUEVEDO_NORTE")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        User user = activeUser("cliente@test.com", "Passw0rd!");
        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(user));
        stubTokenIssuance();

        AuthResponse response = authService.login("cliente@test.com", "Passw0rd!");

        assertThat(response.accessToken()).isEqualTo("fake-access-token");
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void loginFailsWithWrongPassword() {
        User user = activeUser("cliente@test.com", "Passw0rd!");
        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("cliente@test.com", "otra-clave"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginFailsWhenUserIsInactive() {
        User user = activeUser("cliente@test.com", "Passw0rd!");
        user.setActive(false);
        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("cliente@test.com", "Passw0rd!"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshRotatesTokenAndRevokesThePreviousOne() {
        User user = activeUser("cliente@test.com", "Passw0rd!");
        UUID userId = user.getId();
        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hash-viejo")
                .issuedAt(OffsetDateTime.now().minusDays(1))
                .expiresAt(OffsetDateTime.now().plusDays(6))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        stubTokenIssuance();

        AuthResponse response = authService.refresh("token-crudo-viejo");

        assertThat(response.accessToken()).isEqualTo("fake-access-token");
        assertThat(existing.isRevoked()).isTrue();
        assertThat(existing.getReplacedBy()).isNotNull();
    }

    @Test
    void refreshWithAlreadyRevokedTokenTriggersMassRevocation() {
        UUID userId = UUID.randomUUID();
        RefreshToken alreadyRevoked = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hash-robado")
                .issuedAt(OffsetDateTime.now().minusDays(2))
                .expiresAt(OffsetDateTime.now().plusDays(5))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(alreadyRevoked));

        RefreshToken otherActiveToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("otro-hash-activo")
                .issuedAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId)).thenReturn(List.of(otherActiveToken));

        assertThatThrownBy(() -> authService.refresh("token-robado"))
                .isInstanceOf(TokenReuseDetectedException.class);

        assertThat(otherActiveToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(List.of(otherActiveToken));
    }

    private void stubTokenIssuance() {
        when(jwtService.generateAccessToken(any(User.class)))
                .thenReturn(new JwtService.IssuedAccessToken("fake-access-token", OffsetDateTime.now().plusMinutes(15)));
        when(jwtService.refreshTokenTtl()).thenReturn(Duration.ofDays(7));
    }

    private User activeUser(String email, String rawPassword) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName("Test User")
                .role(Role.CLIENTE)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
