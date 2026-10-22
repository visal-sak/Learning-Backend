package co.istad.visal.e_learning.features.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Request / response shapes for the quiz AI helper. */
public final class AiHelperDtos {

    private AiHelperDtos() {
    }

    /**
     * What the quiz screen may offer right now.
     * configured = an API key is set on the server; hints* apply to the current (unsubmitted) attempt.
     */
    public record AiHelperStatus(boolean configured, boolean hintsEnabled, int hintsPerAttempt, int hintsUsed,
                                 int hintsLeft, boolean explainEnabled, double hintPenalty) {
    }

    public record HintRequest(
            @NotBlank String questionId,
            @Pattern(regexp = "en|km") String language
    ) {
    }

    public record HintResponse(String hint, int hintsUsed, int hintsLeft) {
    }

    /**
     * The student's own answer to explain. Lesson quizzes send option indexes (selected) or text;
     * the final quiz sends option ids (selectedOptionIds) or text.
     */
    public record ExplainRequest(
            @NotBlank String questionId,
            List<Integer> selected,
            List<UUID> selectedOptionIds,
            @Size(max = 500) String text,
            @Pattern(regexp = "en|km") String language
    ) {
    }

    public record ExplainResponse(String explanation) {
    }
}
