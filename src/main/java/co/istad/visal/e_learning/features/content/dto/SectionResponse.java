package co.istad.visal.e_learning.features.content.dto;

import java.util.List;
import java.util.UUID;

public record SectionResponse(
        UUID id,
        String title,
        int position,
        int totalMinutes,
        List<LessonSummaryResponse> lessons
) {
}
