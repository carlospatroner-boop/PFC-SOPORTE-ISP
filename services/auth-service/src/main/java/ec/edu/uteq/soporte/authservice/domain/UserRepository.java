package ec.edu.uteq.soporte.authservice.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto (patron Repository) del dominio para User -- mismo criterio que
 * domain/TicketRepository.java en ticket-service: el dominio y la capa de aplicacion no
 * conocen JPA ni SQL. La implementacion real vive en
 * infrastructure/persistence/UserRepositoryAdapter.java.
 */
public interface UserRepository {

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    List<User> findAll();

    User save(User user);
}
