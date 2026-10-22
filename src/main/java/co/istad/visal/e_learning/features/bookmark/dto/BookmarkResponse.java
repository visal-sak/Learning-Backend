package co.istad.visal.e_learning.features.bookmark.dto;

import java.time.Instant;
import java.util.UUID;

/** A saved lesson with enough context to list it ("Course › Section › Lesson") and link to it. */
public record BookmarkResponse(
        UUID lessonId,
        String lessonTitle,
        Integer durationMinutes,
        UUID sectionId,
        String sectionTitle,
        UUID courseId,
        String courseTitle,
        String courseThumbnailUrl,
        Instant createdAt
) {
}
