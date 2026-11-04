package co.istad.visal.e_learning.features.user.dto;

import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * By email rather than id: the local users row only exists after a first
 * login, so someone who has registered but never signed in has no id yet.
 */
public record AssignRoleRequest(

        @NotBlank(message = "Email is required")
        @Email
        String email,

        @NotNull(message = "Role is required")
        KeycloakRoleEnum role
) {
}
