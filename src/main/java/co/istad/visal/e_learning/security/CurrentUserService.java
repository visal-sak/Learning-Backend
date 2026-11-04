package co.istad.visal.e_learning.security;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.features.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Resolves the Keycloak sub claim to a local User row, creating one on first
 * sight. Every service that needs "who is calling" goes through here, so the
 * lookup lives in one place instead of scattered across the codebase.
 */
@Service
@Slf4j
public class CurrentUserService {

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUsers;
    private final TransactionTemplate ownTransaction;

    public CurrentUserService(UserRepository userRepository, KeycloakUserService keycloakUsers,
                              PlatformTransactionManager transactions) {
        this.userRepository = userRepository;
        this.keycloakUsers = keycloakUsers;
        this.ownTransaction = new TransactionTemplate(transactions);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
     *
     * This is also where people who signed up on Keycloak's registration page
     * join the default group and get their email marked verified, as the
     * backend's own registration does. Their roles arrive with the next token.
     * A Keycloak hiccup here must not fail the login, so it is logged and skipped.
     *
     * The row is written in its own transaction. Most reads run inside a
     * read-only one, where a save is silently never flushed, so the row would
     * not exist until the first write, and any problem with it would surface
     * there as a 500 on something unrelated, like creating a quiz.
     */
    private User provision(Jwt jwt) {
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        String name = Optional.ofNullable(jwt.getClaimAsString("name"))
                .orElseGet(() -> Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
                        .orElse("Learner"));

        User user;
        try {
            user = ownTransaction.execute(status -> userRepository.findByEmailIgnoreCase(email)
                    .map(existing -> relink(existing, sub))
                    .orElseGet(() -> userRepository.save(new User(sub, email, name))));
        } catch (DataIntegrityViolationException ex) {
            // Two first requests at once: the other one created the row
            return userRepository.findByKeycloakSub(sub).orElseThrow(() -> ex);
        }

        attempt("join the default group", jwt, keycloakUsers::joinDefaultGroup);
        attempt("mark the email verified", jwt, keycloakUsers::markEmailVerified);

        // Reloaded so the caller holds an entity from its own transaction
        return userRepository.findByKeycloakSub(sub).orElse(user);
    }

    /**
     * The email already has a row under another sub, which happens when an
     * account is deleted in Keycloak and registered again. It keeps its row, so
     * courses and progress survive. Only when the old account is really gone:
     * otherwise this would hand one person's data to another.
     */
    private User relink(User existing, String sub) {
        if (keycloakUsers.accountExists(existing.getKeycloakSub())) {
            throw new ConflictException("This email is already linked to another account");
        }
        existing.relinkTo(sub);
        return userRepository.save(existing);
    }

    /** Separate attempts, so one failing does not skip the other. */
    private void attempt(String action, Jwt jwt, Consumer<String> step) {
        try {
            step.accept(jwt.getSubject());
        } catch (RuntimeException ex) {
            log.warn("Could not {} for {}: {}", action, jwt.getSubject(), ex.getMessage());
        }
    }
}