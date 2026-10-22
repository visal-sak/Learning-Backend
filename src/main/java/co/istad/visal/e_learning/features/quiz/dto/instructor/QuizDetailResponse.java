package co.istad.visal.e_learning.features.quiz.dto.instructor;

import java.util.List;
import java.util.UUID;

/** The quiz builder view. Answers included. */
public record QuizDetailResponse(
        UUID id,
        UUID courseId,
        String title,
        String instructions,
        short passMark,
        Short maxAttempts,
        Short timeLimitMinutes,
        int totalPoints,
        List<QuestionResponse> questions
) {
}
