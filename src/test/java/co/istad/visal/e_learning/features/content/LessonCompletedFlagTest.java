package co.istad.visal.e_learning.features.content;

import co.istad.visal.e_learning.features.content.dto.LessonSummaryResponse;
import co.istad.visal.e_learning.features.content.dto.SectionResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LessonCompletedFlagTest {

    private static LessonSummaryResponse lesson(UUID id, int position) {
        return new LessonSummaryResponse(id, "Lesson " + position, 10, false, position, false, "LESSON", 0, false);
    }

    @Test
    void marksOnlyTheStudentsFinishedLessons() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        SectionResponse section = new SectionResponse(UUID.randomUUID(), "Basics", 1, 20, List.of(lesson(first, 1), lesson(second, 2)));

        SectionResponse marked = SectionResponse.markProgress(section, Set.of(first));

        assertEquals(List.of(true, false), marked.lessons().stream().map(LessonSummaryResponse::completed).toList());
    }

    @Test
    void aVisitorWithNoProgressHasNothingMarked() {
        SectionResponse section = new SectionResponse(UUID.randomUUID(), "Basics", 1, 10, List.of(lesson(UUID.randomUUID(), 1)));

        assertEquals(false, SectionResponse.markProgress(section, Set.of()).lessons().getFirst().completed());
    }
}
