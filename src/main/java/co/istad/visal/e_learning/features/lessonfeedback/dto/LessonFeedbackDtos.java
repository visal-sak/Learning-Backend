package co.istad.visal.e_learning.features.lessonfeedback.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class LessonFeedbackDtos {

    private LessonFeedbackDtos() {
    }

    public record SaveFeedbackRequest(
            @NotNull(message = "Choose helpful or not helpful")
            Boolean helpful,
            @Size(max = 500)
            String comment
    ) {
    }

    /** The student's own answer. helpful is null until they have given one. */
    public record MyFeedback(Boolean helpful, String comment) {
    }

    /** What staff see. */
    public record FeedbackResponse(UUID id, UUID lessonId, String lessonTitle, UUID courseId, String courseTitle,
                                   String studentName, boolean helpful, String comment, Instant updatedAt) {
    }
}
