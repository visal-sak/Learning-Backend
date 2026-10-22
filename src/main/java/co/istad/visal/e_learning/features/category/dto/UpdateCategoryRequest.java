package co.istad.visal.e_learning.features.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for PUT /api/categories/{id}.
 * Kept separate from the create request on purpose: the two diverge as soon
 * as one field becomes immutable after creation, and sharing one DTO makes
 * that impossible to express.
 */
public record UpdateCategoryRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 80, message = "Category name must be 80 characters or fewer")
        String name,

        @Size(max = 300, message = "Description must be 300 characters or fewer")
        String description
) {
}
