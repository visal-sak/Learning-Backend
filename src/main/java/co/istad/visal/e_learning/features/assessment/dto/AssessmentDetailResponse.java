package co.istad.visal.e_learning.features.assessment.dto;

import co.istad.visal.e_learning.features.quiz.QuestionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The results dashboard for one course quiz.
 *   distribution — attempts per score band (0–9, 10–19 … 90–100)
 *   questions    — how often each question is answered correctly, hardest first
 *   students     — each student's best attempt
 */
public record AssessmentDetailResponse(
        AssessmentSummaryResponse summary,
        List<Band> distribution,
        List<QuestionStat> questions,
        List<StudentResult> students
) {
    public record Band(int from, int to, int attempts) {
    }

    public record QuestionStat(
            UUID questionId,
            String text,
            QuestionType type,
            int answered,
            int correct,
            Integer correctRate
    ) {
    }

    public record StudentResult(
            UUID userId,
            String displayName,
            String email,
            String avatarUrl,
            int attempts,
            short bestScore,
            boolean passed,
            Instant lastAttemptAt
    ) {
    }
}
