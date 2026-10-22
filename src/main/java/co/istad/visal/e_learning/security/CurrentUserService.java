package co.istad.visal.e_learning.security;

import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.features.user.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resolves the Keycloak sub claim to a local User row, creating one on first
 * sight. Every service that needs "who is calling" goes through here, so the
 * lookup lives in one place instead of scattered across the codebase.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User require() {
        return current().orElseThrow(() -> new NotFoundException("No authenticated user on this request"));
    }

    @Transactional
    public Optional<User> current() {
        return AuthUtils.currentJwt()
                .map(jwt -> userRepository.findByKeycloakSub(jwt.getSubject())
                        .orElseGet(() -> provision(jwt)));
    }

    public boolean isAuthenticated() {
        return AuthUtils.isAuthenticated();
    }

    /**
     * No ROLE_ prefix. JwtRoleConverter maps realm roles with
     * SimpleGrantedAuthority::new, so the authority is plain "ADMIN".
     * Comparing against "ROLE_ADMIN" silently never matches, and callers
     * quietly fall through to the instructor rules instead of erroring —
     * which is how a legal admin transition ends up rejected as illegal.
     */
    public boolean hasRole(KeycloakRoleEnum role) {
        return AuthUtils.hasRole(role);
    }

    /**
     * First login: Keycloak already holds the identity, we just need a local
     * row to hang profile fields and foreign keys off.
     */
    private User provision(Jwt jwt) {
        String email = jwt.getClaimAsString("email");

        String name = Optional.ofNullable(jwt.getClaimAsString("name"))
                .orElseGet(() -> Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
                        .orElse("Learner"));

        return userRepository.save(new User(jwt.getSubject(), email, name));
    }
}