package co.istad.visal.e_learning.features.assessment.dto;

import java.time.Instant;
import java.util.UUID;

/** One course's quiz at a glance. Rates are 0–100; null when there are no attempts yet. */
public record AssessmentSummaryResponse(
        UUID courseId,
        String courseTitle,
        String thumbnailUrl,
        String instructorName,
        UUID quizId,
        String quizTitle,
        short passMark,
        int questionCount,
        int attempts,
        int students,
        Integer passRate,
        Integer averageScore,
        Instant lastAttemptAt
) {
}
