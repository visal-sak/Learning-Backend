package co.istad.visal.e_learning.features.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** The lookup that starts every authenticated request. */
    Optional<User> findByKeycloakSub(String keycloakSub);

    Optional<User> findByEmailIgnoreCase(String email);
}
