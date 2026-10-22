package co.istad.visal.e_learning.features.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSectionRequest(

        @NotNull UUID courseId,

        @NotBlank(message = "Section title is required")
        @Size(max = 200)
        String title
) {
}
