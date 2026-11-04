package co.istad.visal.e_learning.features.user.dto;

import java.util.List;

/** A row in the admin "User roles" table. `id` is the Keycloak id: not everyone has a local users row yet. */
public record UserAccountResponse(
        String id,
        String username,
        String email,
        String name,
        boolean enabled,
        List<String> roles
) {
}
