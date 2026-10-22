package co.istad.visal.e_learning.features.quiz.dto.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Option ids / typed text and nothing else. No score field: a client-computed
 * score is not evidence of anything, and accepting one means the quiz grades itself.
 */
public record SubmitAttemptRequest(
        @NotEmpty(message = "Answer at least one question")
        @Valid
        List<Answer> answers
) {
    /**
     * Fill the field that matches the question type:
     *   SINGLE_CHOICE / TRUE_FALSE → selectedOptionId
     *   MULTIPLE_CHOICE            → selectedOptionIds
     *   SHORT_ANSWER               → textAnswer
     */
    public record Answer(
            @NotNull UUID questionId,
            UUID selectedOptionId,
            List<UUID> selectedOptionIds,
            String textAnswer
    ) {
    }
}
