package co.istad.visal.e_learning.features.enrollment.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * What an instructor sees about the students in their own course. No email,
 * no other courses — only what is needed to teach this one.
 */
public record StudentProgressResponse(
        UUID userId,
        String displayName,
        String avatarUrl,
        int completedLessons,
        int totalLessons,
        int percentComplete,
        Instant enrolledAt,
        Instant lastAccessedAt
) {
}
