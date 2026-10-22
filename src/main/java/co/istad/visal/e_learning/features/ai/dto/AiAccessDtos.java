package co.istad.visal.e_learning.features.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AiAccessDtos {

    private AiAccessDtos() {
    }

    /** Give one or more students extra Bunny AI help on a lesson. */
    public record GrantAiAccessRequest(
            @NotNull UUID lessonId,
            @NotEmpty @Size(max = 200) List<UUID> userIds,
            @Min(0) @Max(50) int extraQuestions,
            @Min(0) @Max(20) int extraHints,
            @Size(max = 500) String note,
            /* also send an email (in-app notification is always sent) */
            boolean sendEmail
    ) {
    }

    public record AiAccessGrantResponse(UUID id, UUID userId, String studentName, String email, UUID courseId,
                                        UUID lessonId, String lessonTitle, int extraQuestions, int extraHints,
                                        String note, String grantedByName, Instant createdAt,
                                        /* true when an email was queued for this student */
                                        boolean emailQueued) {
    }
}
