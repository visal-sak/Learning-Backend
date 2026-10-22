package co.istad.visal.e_learning.features.course.dto;

import co.istad.visal.e_learning.features.content.dto.SectionResponse;
import co.istad.visal.e_learning.features.course.CourseLevel;
import co.istad.visal.e_learning.features.course.CourseStatus;
import co.istad.visal.e_learning.features.user.dto.PublicProfileResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The course page. Sections and lessons are always included — the curriculum
 * outline is public marketing. What varies by caller is whether each lesson
 * carries its videoUrl, and that decision lives in the lesson mapper.
 */
public record CourseDetailResponse(
        UUID id,
        String title,
        String slug,
        String summary,
        String description,
        String thumbnailUrl,
        CourseLevel level,
        CourseStatus status,
        String categoryName,
        UUID categoryId,
        PublicProfileResponse instructor,
        List<SectionResponse> sections,
        int lessonCount,
        int totalMinutes,
        boolean enrolled,
        boolean editable,
        Integer percentComplete,
        String reviewNote,
        Instant publishedAt,
        boolean requireQuizPass,
        boolean requireFinalQuiz,
        boolean certificateEnabled,
        /* Questions in the course's final quiz (0 = no final quiz) */
        int finalQuizQuestions
) {
}
