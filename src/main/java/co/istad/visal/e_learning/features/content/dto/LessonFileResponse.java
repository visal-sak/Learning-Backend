package co.istad.visal.e_learning.features.content.dto;

import java.util.UUID;

public record LessonFileResponse(
        UUID id,
        String fileName,
        String mimeType,
        Long sizeBytes,
        String downloadUrl
) {
}
