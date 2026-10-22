package co.istad.visal.e_learning.features.course.dto;

import co.istad.visal.e_learning.features.course.CourseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCourseRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200)
        String title,

        UUID categoryId,

        @Size(max = 500)
        String summary,

        String description,

        String thumbnailUrl,

        CourseLevel level,

        /* Settings — null leaves the current value */
        Boolean requireQuizPass,
        Boolean requireFinalQuiz,
        Boolean certificateEnabled,

        /* AI helper in quizzes — null leaves the current value */
        Boolean aiHelperEnabled,
        @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(10) Integer aiHintsPerAttempt,
        Boolean aiInFinalQuiz,
        Boolean aiExplainEnabled,
        @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.DecimalMax("1") Double aiHintPenalty
) {
}
