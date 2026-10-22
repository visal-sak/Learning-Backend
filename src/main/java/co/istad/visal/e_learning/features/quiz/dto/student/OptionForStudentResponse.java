package co.istad.visal.e_learning.features.quiz.dto.student;

import java.util.UUID;

/**
 * Id and text. Nothing else.
 *
 * There is no isCorrect field here and there never should be. That is the
 * entire reason this package exists separately from dto.instructor — a
 * conditional on a shared DTO is a rule someone forgets on the next endpoint.
 */
public record OptionForStudentResponse(
        UUID id,
        String text
) {
}
