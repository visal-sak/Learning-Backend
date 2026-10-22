package co.istad.visal.e_learning.features.quiz.dto.instructor;

import co.istad.visal.e_learning.features.quiz.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

/**
 * One request covers create and update, because a question is only meaningful
 * together with its full option set — patching options individually invites
 * states with no correct answer at all.
 *
 * What is required depends on the type (checked in the service):
 *   SINGLE_CHOICE    2–6 options, exactly one correct
 *   MULTIPLE_CHOICE  2–6 options, two or more correct
 *   TRUE_FALSE       correctAnswer; the "True"/"False" options are seeded by the service
 *   SHORT_ANSWER     answerKey (accepted answers, one per line); no options
 */
public record SaveQuestionRequest(
        UUID quizId,

        @NotBlank(message = "Question text is required")
        String text,

        String explanation,

        @Positive
        short points,

        /** Null means SINGLE_CHOICE, so older clients keep working. */
        QuestionType type,

        @Size(max = 6, message = "Provide at most six options")
        @Valid
        List<OptionInput> options,

        /** TRUE_FALSE only: true when "True" is the correct answer. */
        Boolean correctAnswer,

        /** SHORT_ANSWER only: accepted answers, one per line. */
        String answerKey
) {
    public record OptionInput(
            UUID id,
            @NotBlank(message = "Option text is required") String text,
            boolean correct
    ) {
    }
}
