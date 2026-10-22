package co.istad.visal.e_learning.features.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * No courseId and no userId. The course comes from the path, the user from
 * the token, and the enrollment is looked up from the pair.
 */
public record SaveReviewRequest(

        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        short rating,

        @Size(max = 1000, message = "Keep the comment under 1000 characters")
        String comment
) {
}
