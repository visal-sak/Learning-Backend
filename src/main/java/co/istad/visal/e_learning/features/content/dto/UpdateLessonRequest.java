package co.istad.visal.e_learning.features.content.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateLessonRequest(

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
