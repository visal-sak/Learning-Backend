package co.istad.visal.e_learning.features.content.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Drag and drop sends the whole ordered list rather than one moved item.
 * Positions are then rewritten in a single transaction, which avoids the
 * gaps and duplicates that incremental swaps produce.
 */
public record ReorderRequest(
        @NotEmpty(message = "Send the full ordered list of ids")
        List<UUID> orderedIds
) {
}
