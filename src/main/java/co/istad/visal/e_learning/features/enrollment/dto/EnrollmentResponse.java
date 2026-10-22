package co.istad.visal.e_learning.features.enrollment.dto;

import co.istad.visal.e_learning.features.enrollment.EnrollmentStatus;

import java.time.Instant;
import java.util.UUID;

/** A row in "My learning". */
public record EnrollmentResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        String courseSlug,
        String thumbnailUrl,
        String instructorName,
        EnrollmentStatus status,
        int completedLessons,
        int totalLessons,
        int percentComplete,
        UUID nextLessonId,
        Instant enrolledAt,
        Instant lastAccessedAt
) {
}
