package co.istad.visal.e_learning.features.quiz.dto.instructor;

import co.istad.visal.e_learning.features.quiz.QuestionType;

import java.util.List;
import java.util.UUID;

/** Owner/admin view: carries the answer key (options' correct flags and answerKey). */
public record QuestionResponse(
        UUID id,
        String text,
        String explanation,
        short points,
        int position,
        QuestionType type,
        String answerKey,
        List<AnswerOptionResponse> options
) {
}
