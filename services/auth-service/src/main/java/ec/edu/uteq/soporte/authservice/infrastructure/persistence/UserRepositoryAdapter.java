package ec.edu.uteq.soporte.authservice.infrastructure.persistence;

import ec.edu.uteq.soporte.authservice.domain.Role;
import ec.edu.uteq.soporte.authservice.domain.User;
import ec.edu.uteq.soporte.authservice.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador (patron Repository) que implementa el puerto de dominio UserRepository
 * delegando en Spring Data JPA -- mismo criterio exacto que TicketRepositoryAdapter en
 * ticket-service. El id lo genera Hibernate (GenerationType.UUID en UserJpaEntity), no
 * este adaptador -- a diferencia de IncidenciaRepositoryAdapter en ticket-service, que
 * si genera el id a mano porque su estrategia de persistencia es distinta.
 */
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository jpaRepository;
    private final UserMapper mapper;

    public UserRepositoryAdapter(SpringDataUserRepository jpaRepository, UserMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByRole(Role role) {
        return jpaRepository.existsByRole(role);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = jpaRepository.save(mapper.toEntity(user));
        return mapper.toDomain(saved);
    }
}
