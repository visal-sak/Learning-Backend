package co.istad.visal.e_learning.features.quiz;

/**
 * How a question is answered and graded.
 *
 * SINGLE_CHOICE    options, exactly 1 correct        → picked id is the correct one
 * MULTIPLE_CHOICE  options, 2+ correct               → picked set equals the correct set
 * TRUE_FALSE       2 options, seeded by the service  → same as single choice
 * SHORT_ANSWER     no options, answer key on the question → normalized text match
 */
public enum QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    SHORT_ANSWER
}
