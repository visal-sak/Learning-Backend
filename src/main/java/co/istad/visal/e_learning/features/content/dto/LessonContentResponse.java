package co.istad.visal.e_learning.features.content.dto;

import java.util.List;
import java.util.UUID;

/**
 * Returned only after CourseAccessService.requireCanViewContent passes.
 * videoUrl here is a signed, short-lived link — never the stored key.
 */
public record LessonContentResponse(
        UUID id,
        UUID courseId,
        String title,
        String content,
        List<LessonBlock> blocks,
        String videoUrl,
        int durationMinutes,
        boolean preview,
        boolean completed,
        List<LessonFileResponse> files,
        UUID nextLessonId,
        String kind,
        int questionCount
) {
}
