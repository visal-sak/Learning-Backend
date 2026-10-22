package co.istad.visal.e_learning.features.user.dto;

import java.util.UUID;

/**
 * What other people see — an instructor byline on a course card, for example.
 * No email. Two DTOs rather than one conditional mapper, because the
 * conditional is what gets forgotten on the next endpoint.
 */
public record PublicProfileResponse(
        UUID id,
        String displayName,
        String headline,
        String avatarUrl
) {
}
