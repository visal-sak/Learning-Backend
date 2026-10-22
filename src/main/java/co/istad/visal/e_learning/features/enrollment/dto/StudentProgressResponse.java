package co.istad.visal.e_learning.features.enrollment.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * What an instructor sees about the students in their own course — only what is needed to teach this one.
 * email is filled in for admins only (instructors get null).
 */
public record StudentProgressResponse(
        UUID userId,
        String displayName,
        String avatarUrl,
        int completedLessons,
        int totalLessons,
        int percentComplete,
        Instant enrolledAt,
        Instant lastAccessedAt,
        String email,
        /* ACTIVE | COMPLETED — the course was finished (even if lessons were added later) */
        String status,
        Instant completedAt,
        /* the student's certificate for this course, or null */
        String certificateSerial
) {
}
