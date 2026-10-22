package co.istad.visal.e_learning.features.quiz.dto.instructor;

import java.util.UUID;

/** Carries isCorrect. Only ever returned to the course owner or an admin. */
public record AnswerOptionResponse(
        UUID id,
        String text,
        boolean correct,
        int position
) {
}
