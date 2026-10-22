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
        Boolean certificateEnabled
) {
}
