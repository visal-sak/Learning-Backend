package co.istad.visal.e_learning.features.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSectionRequest(

        @NotBlank(message = "Section title is required")
        @Size(max = 200)
        String title
) {
}
