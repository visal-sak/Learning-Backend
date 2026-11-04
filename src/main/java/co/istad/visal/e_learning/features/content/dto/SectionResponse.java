package co.istad.visal.e_learning.features.content.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SectionResponse(
        UUID id,
        String title,
        int position,
        int totalMinutes,
        List<LessonSummaryResponse> lessons
) {
    /**
     * The same outline with one student's progress marked on it. Static because
     * MapStruct reads an instance method returning this type as a fluent setter.
     */
    public static SectionResponse markProgress(SectionResponse s, Set<UUID> doneLessonIds) {
        return new SectionResponse(s.id(), s.title(), s.position(), s.totalMinutes(), s.lessons().stream()
                .map(l -> new LessonSummaryResponse(l.id(), l.title(), l.durationMinutes(), l.preview(), l.position(),
                        l.locked(), l.kind(), l.questionCount(), doneLessonIds.contains(l.id())))
                .toList());
    }
}
