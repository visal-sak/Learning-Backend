package co.istad.visal.e_learning.features.quiz.dto.student;

import co.istad.visal.e_learning.features.quiz.QuestionType;

import java.util.List;
import java.util.UUID;

/**
 * No explanation field either — that is revealed with the result. No answerKey:
 * a SHORT_ANSWER question reaches the student as text + type with no options.
 */
public record QuestionForStudentResponse(
        UUID id,
        String text,
        short points,
        QuestionType type,
        List<OptionForStudentResponse> options
) {
}
