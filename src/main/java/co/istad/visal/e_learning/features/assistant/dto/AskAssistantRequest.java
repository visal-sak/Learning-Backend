package co.istad.visal.e_learning.features.assistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The chat so far, oldest first, ending with the student's new question.
 * The server keeps no chat state; the client sends the recent history each time.
 */
public record AskAssistantRequest(
        @NotEmpty(message = "Ask a question") @Size(max = 20) @Valid List<Turn> messages,
        /** "en" or "km" — the language the tutor should answer in. */
        @Pattern(regexp = "en|km") String language
) {
    public record Turn(
            @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 4000) String content
    ) {
    }
}
