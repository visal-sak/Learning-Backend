package co.istad.visal.e_learning.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

public final class AuthUtils {

    private AuthUtils() {
    }

    public static Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * The Keycloak sub claim. Resolve this against users.keycloak_sub — never
     * trust a user id sent in a request body.
     */
    public static String extractUserId() {
        Authentication auth = getAuth();

        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return jwtAuth.getToken().getSubject();
    }

    /** Empty rather than throwing — for endpoints that also serve anonymous callers. */
    public static Optional<Jwt> currentJwt() {
        Authentication auth = getAuth();

        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    public static Optional<String> currentUserId() {
        return currentJwt().map(Jwt::getSubject);
    }

    public static String extractJwt() {
        return currentJwt()
                .map(Jwt::getTokenValue)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "You have been unauthorized"));
    }

    /**
     * Authorities carry no ROLE_ prefix — JwtRoleConverter maps realm roles
     * with SimpleGrantedAuthority::new — so this compares the raw role name.
     */
    public static boolean hasRole(KeycloakRoleEnum role) {
        Authentication auth = getAuth();

        if (auth == null) {
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(role.name()));
    }

    public static boolean isAuthenticated() {
        Authentication auth = getAuth();
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }
}