package co.istad.visal.e_learning.features.review.dto;

import java.util.Map;

/**
 * Computed on read, never stored. A stored average is wrong the moment
 * someone edits a review, and nothing tells you.
 */
public record RatingSummaryResponse(
        double average,
        long total,
        /** Rating value to how many people gave it, for the bar chart. */
        Map<Short, Long> distribution
) {
}
