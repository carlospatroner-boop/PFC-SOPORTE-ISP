package ec.edu.uteq.soporte.authservice.infrastructure.persistence;

import ec.edu.uteq.soporte.authservice.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador tecnico de Spring Data JPA -- SOLO lo usa UserRepositoryAdapter, mismo
 * criterio de encapsulamiento que SpringDataTicketRepository en ticket-service.
 */
interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}
