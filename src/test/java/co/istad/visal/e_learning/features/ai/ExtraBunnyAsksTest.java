package co.istad.visal.e_learning.features.ai;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExtraBunnyAsksTest {

    @Test
    void grantsSavedAsTutorQuestionsCountInTheQuiz() {
        AiAccessGrantRepository grants = mock(AiAccessGrantRepository.class);
        UUID student = UUID.randomUUID();
        UUID lesson = UUID.randomUUID();
        when(grants.extraQuestions(student, lesson)).thenReturn(2);
        when(grants.extraHints(student, lesson)).thenReturn(0);

        when(grants.extraAsks(student, lesson)).thenCallRealMethod();

        assertEquals(2, grants.extraAsks(student, lesson));
    }

    @Test
    void bothKindsAddUp() {
        AiAccessGrantRepository grants = mock(AiAccessGrantRepository.class);
        UUID student = UUID.randomUUID();
        UUID lesson = UUID.randomUUID();
        when(grants.extraQuestions(student, lesson)).thenReturn(1);
        when(grants.extraHints(student, lesson)).thenReturn(1);

        when(grants.extraAsks(student, lesson)).thenCallRealMethod();

        assertEquals(2, grants.extraAsks(student, lesson));
    }
}
