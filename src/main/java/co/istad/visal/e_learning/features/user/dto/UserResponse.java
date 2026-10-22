package co.istad.visal.e_learning.features.user.dto;

import java.time.Instant;
import java.util.UUID;

/** Own profile. Includes email. */
public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String headline,
        String bio,
        String avatarUrl,
        String gender,
        String facebookProfile,
        String telegramProfile,
        Instant createdAt
) {
}