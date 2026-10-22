package co.istad.visal.e_learning.features.enrollment.dto;

import java.util.UUID;

/** Returned after marking a lesson complete, so the client can update in place. */
public record ProgressResponse(
        UUID enrollmentId,
        UUID lessonId,
        int completedLessons,
        int totalLessons,
        int percentComplete,
        boolean courseCompleted,
        /** Set when this completion earned the certificate (otherwise null). */
        String certificateSerial
) {
}
