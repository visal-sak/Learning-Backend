package co.istad.visal.e_learning.features.note.dto;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        UUID lessonId,
        String lessonTitle,
        UUID courseId,
        String courseTitle,
        Integer timestampSeconds,
        String body,
        Instant createdAt,
        Instant updatedAt
) {
}
