package co.istad.visal.e_learning.features.assistant.dto;

/**
 * enabled = an AI key is configured; limit / used / left = tutor questions for this lesson;
 * extra = how many of those were granted for this lesson by an admin or instructor.
 */
public record TutorStatusResponse(boolean enabled, int limit, int used, int left, int extra) {
}
