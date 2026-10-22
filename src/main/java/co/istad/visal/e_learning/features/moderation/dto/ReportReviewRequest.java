package co.istad.visal.e_learning.features.moderation.dto;

import co.istad.visal.e_learning.features.moderation.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportReviewRequest(
        @NotNull(message = "Choose a reason") ReportReason reason,
        @Size(max = 500) String details
) {
}
