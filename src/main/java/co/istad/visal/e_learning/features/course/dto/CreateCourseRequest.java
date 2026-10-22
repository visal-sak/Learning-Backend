package co.istad.visal.e_learning.features.course.dto;

import co.istad.visal.e_learning.features.course.CourseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * No instructorId field. The owner is taken from the token — accepting it in
 * the body would let anyone create a course in someone else's name.
 *
 * thumbnailUrl is the uri returned by POST /api/v1/medias. Upload first,
 * then create the course with the uri you got back.
 */
public record CreateCourseRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200)
        String title,

        @NotNull(message = "Pick a category")
        UUID categoryId,

        @Size(max = 500)
        String summary,

        String description,

        String thumbnailUrl,

        CourseLevel level
) {
}