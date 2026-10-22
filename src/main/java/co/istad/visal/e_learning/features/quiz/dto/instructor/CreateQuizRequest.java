package co.istad.visal.e_learning.features.quiz.dto.instructor;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateQuizRequest(

        @NotNull UUID courseId,

        @NotBlank(message = "Quiz title is required")
        @Size(max = 200)
        String title,

        String instructions,

        @Min(0) @Max(100)
        short passMark,

        @Positive
        Short maxAttempts,

        @Positive
        Short timeLimitMinutes
) {
}
