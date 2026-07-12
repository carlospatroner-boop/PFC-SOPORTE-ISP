package ec.edu.uteq.soporte.authservice.config;

import ec.edu.uteq.soporte.authservice.domain.Role;
import ec.edu.uteq.soporte.authservice.domain.User;
import ec.edu.uteq.soporte.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.logging.Logger;

/**
 * Crea una cuenta ADMIN por defecto en el primer arranque si todavia no existe
 * ninguna -- evita tener que calcular un hash BCrypt a mano e insertarlo por SQL
 * solo para poder probar el endpoint /api/v1/auth/admin/users.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = Logger.getLogger(AdminBootstrap.class.getName());

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapEmail;
    private final String bootstrapPassword;

    public AdminBootstrap(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${admin.bootstrap.email}") String bootstrapEmail,
                           @Value("${admin.bootstrap.password}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        User admin = User.builder()
                .email(bootstrapEmail)
                .passwordHash(passwordEncoder.encode(bootstrapPassword))
                .fullName("Administrador")
                .role(Role.ADMIN)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();
        userRepository.save(admin);
        LOGGER.warning(() -> "Cuenta ADMIN creada automaticamente (" + bootstrapEmail
                + "). Cambiar la contrasena por defecto en cualquier entorno compartido.");
    }
}
