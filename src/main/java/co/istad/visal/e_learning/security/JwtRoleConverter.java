package co.istad.visal.e_learning.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pulls realm roles out of realm_access.roles and turns them into
 * authorities.
 *
 * Note there is no ROLE_ prefix. That means every check in the codebase must
 * use hasAuthority('ADMIN'), not hasRole('ADMIN') — hasRole silently prepends
 * ROLE_ and would never match. Pick one convention and hold to it.
 */
@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Collection<String>> realmAccess = jwt.getClaim(REALM_ACCESS);

        Collection<String> roles = new HashSet<>();
        if (realmAccess != null && realmAccess.get(ROLES) != null) {
            roles = realmAccess.get(ROLES);
        }

        Set<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return authorities;
    }
}