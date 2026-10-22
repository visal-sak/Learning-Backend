package co.istad.visal.e_learning.features.content.dto;

import java.util.UUID;

/**
 * The curriculum outline. Public — anonymous visitors see this.
 *
 * Note what is absent: no videoUrl, no content, no file list. Titles are
 * marketing; the payload is not.
 */
public record LessonSummaryResponse(
        UUID id,
        String title,
        int durationMinutes,
        boolean preview,
        int position,
        boolean locked,
        String kind,
        int questionCount
) {
}
