package co.istad.visal.e_learning.features.content.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record CreateLessonRequest(

        @NotNull UUID sectionId,

        @NotBlank(message = "Lesson title is required")
        @Size(max = 200)
        String title,

        String content,

        @Valid
        @Size(max = 300, message = "A lesson can have at most 300 blocks")
        List<LessonBlock> blocks,

        String videoUrl,

        @PositiveOrZero
        int durationMinutes,

        boolean preview,

        /* LESSON (default) or QUIZ — a quiz item in the curriculum */
        @Pattern(regexp = "LESSON|QUIZ", message = "Item kind must be LESSON or QUIZ")
        String kind
) {
}
