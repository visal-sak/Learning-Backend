package co.istad.visal.e_learning.features.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Only the fields your database owns. Email and password are absent on
 * purpose: both belong to Keycloak, and accepting them here would create a
 * second source of truth.
 */
public record UpdateProfileRequest(

        @NotBlank(message = "Display name is required")
        @Size(max = 120)
        String displayName,

        @Size(max = 160)
        String headline,

        @Size(max = 2000)
        String bio,

        String avatarUrl,

        @Size(max = 20)
        String gender,

        @Size(max = 255)
        String facebookProfile,

        @Size(max = 255)
        String telegramProfile
) {
}