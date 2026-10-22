package co.istad.visal.e_learning.features.quiz.dto.student;

import java.util.List;
import java.util.UUID;

/** What GET /quizzes/{id}/take returns. Safe to send to an enrolled student. */
public record QuizTakeResponse(
        UUID id,
        String title,
        String instructions,
        short passMark,
        Short timeLimitMinutes,
        int attemptNo,
        Short maxAttempts,
        List<QuestionForStudentResponse> questions
) {
}
