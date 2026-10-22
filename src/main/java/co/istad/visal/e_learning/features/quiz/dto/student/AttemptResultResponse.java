package co.istad.visal.e_learning.features.quiz.dto.student;

import co.istad.visal.e_learning.features.quiz.QuestionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Returned after grading. Correct answers appear here and only here — the
 * attempt is already submitted, so revealing them costs nothing.
 */
public record AttemptResultResponse(
        UUID attemptId,
        short score,
        boolean passed,
        short passMark,
        int correctCount,
        int totalQuestions,
        int attemptNo,
        Instant submittedAt,
        /** Who took it — for the instructor's attempts list (on a student's own result it is themselves). */
        Student student,
        List<QuestionResultResponse> questions
) {
    public record Student(
            UUID id,
            String displayName,
            String email,
            String avatarUrl
    ) {
    }

    /**
     * selectedOptionText / correctOptionText stay for older clients: lists are
     * joined with ", " and SHORT_ANSWER uses the typed text / accepted answers.
     */
    public record QuestionResultResponse(
            UUID questionId,
            String text,
            QuestionType type,
            UUID selectedOptionId,
            String selectedOptionText,
            UUID correctOptionId,
            String correctOptionText,
            List<UUID> selectedOptionIds,
            List<UUID> correctOptionIds,
            String textAnswer,
            List<String> acceptedAnswers,
            boolean correct,
            String explanation
    ) {
    }
}
