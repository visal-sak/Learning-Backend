package co.istad.visal.e_learning.features.note.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** lessonId is only read on create; timestampSeconds is optional (null = not tied to a moment). */
public record SaveNoteRequest(
        UUID lessonId,
        @Min(0) @Max(86_400) Integer timestampSeconds,
        @NotBlank(message = "Write something in the note") @Size(max = 2000) String body
) {
}
