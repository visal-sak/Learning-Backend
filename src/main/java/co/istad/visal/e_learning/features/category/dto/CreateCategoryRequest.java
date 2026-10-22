package co.istad.visal.e_learning.features.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/categories.
 * The slug is derived server-side from the name — clients do not send it.
 */
public record CreateCategoryRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 80, message = "Category name must be 80 characters or fewer")
        String name,

        @Size(max = 300, message = "Description must be 300 characters or fewer")
        String description
) {
}
