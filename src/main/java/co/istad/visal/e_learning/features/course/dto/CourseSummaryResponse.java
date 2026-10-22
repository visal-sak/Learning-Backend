package co.istad.visal.e_learning.features.course.dto;

import co.istad.visal.e_learning.features.course.CourseLevel;
import co.istad.visal.e_learning.features.course.CourseStatus;

import java.time.Instant;
import java.util.UUID;

/** The catalog card. Deliberately light — no curriculum, no description. */
public record CourseSummaryResponse(
        UUID id,
        String title,
        String slug,
        String summary,
        String thumbnailUrl,
        CourseLevel level,
        CourseStatus status,
        String categoryName,
        String instructorName,
        int lessonCount,
        int totalMinutes,
        Instant publishedAt
) {
}
