package co.istad.visal.e_learning.features.content.dto;

import java.util.UUID;

/**
 * The curriculum outline. Public — anonymous visitors see this.
 *
 * Note what is absent: no videoUrl, no content, no file list. Titles are
 * marketing; the payload is not.
 *
 * completed is the caller's own progress. Without it the player treats every
 * lesson as unfinished on load, locks all but the first and sends the student
 * back to lesson one.
 */
public record LessonSummaryResponse(
        UUID id,
        String title,
        int durationMinutes,
        boolean preview,
        int position,
        boolean locked,
        String kind,
        int questionCount,
        boolean completed
) {
}
