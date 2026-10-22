package co.istad.visal.e_learning.features.review.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Carries the reviewer's display name and avatar but not their email — this
 * is read by anyone browsing the catalog.
 */
public record ReviewResponse(
        UUID id,
        UUID courseId,
        UUID authorId,
        String authorName,
        String authorAvatarUrl,
        short rating,
        String comment,
        int percentCompleteWhenWritten,
        boolean mine,
        Instant createdAt,
        Instant updatedAt
) {
}
