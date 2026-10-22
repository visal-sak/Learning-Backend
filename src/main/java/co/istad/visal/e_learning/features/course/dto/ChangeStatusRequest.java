package co.istad.visal.e_learning.features.course.dto;

import co.istad.visal.e_learning.features.course.CourseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The note is what the instructor reads after a rejection. Optional in the
 * type, required by the service when the target is REJECTED.
 */
public record ChangeStatusRequest(

        @NotNull(message = "Target status is required")
        CourseStatus status,

        @Size(max = 500)
        String note
) {
}
