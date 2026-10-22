package co.istad.visal.e_learning.features.lessoncheck.dto;

import co.istad.visal.e_learning.features.quiz.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * One lesson-check question. Same four types as the course quiz.
 * For students, `correct` on every option and `acceptedAnswers` are null.
 */
public record CheckQuestion(

        /* Stable id so answers can refer to the question; the server fills it in when missing. */
        @Size(max = 40)
        String id,

        @NotNull(message = "Choose a question type")
        QuestionType type,

        @NotBlank(message = "Question text is required")
        @Size(max = 1_000)
        String text,

        /* Shown after answering: why the right answer is right. */
        @Size(max = 2_000)
        String explanation,

        @Valid
        @Size(max = 6, message = "Provide at most six options")
        List<Option> options,

        @Size(max = 10)
        List<@Size(max = 200) String> acceptedAnswers
) {
    public record Option(
            @NotBlank(message = "Option text is required")
            @Size(max = 500)
            String text,

            Boolean correct
    ) {
    }
}
