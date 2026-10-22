package co.istad.visal.e_learning.features.quiz;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** One row per question in an attempt. Enables the per-question result review. */
@Entity
@Table(name = "attempt_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uq_attempt_answers", columnNames = {"attempt_id", "question_id"})
})
public class AttemptAnswer {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false, updatable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, updatable = false)
    private Question question;

    /** Null when the student left the question blank. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private AnswerOption selectedOption;

    /** MULTIPLE_CHOICE: every option the student ticked. */
    @ManyToMany
    @JoinTable(
            name = "attempt_answer_options",
            joinColumns = @JoinColumn(name = "attempt_answer_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id"))
    private Set<AnswerOption> selectedOptions = new LinkedHashSet<>();

    /** SHORT_ANSWER: what the student typed. */
    @Column(name = "text_answer", columnDefinition = "text")
    private String textAnswer;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    protected AttemptAnswer() {
    }

    public AttemptAnswer(QuizAttempt attempt, Question question, AnswerOption selectedOption, boolean correct) {
        this.attempt = attempt;
        this.question = question;
        this.selectedOption = selectedOption;
        this.correct = correct;
    }

    /** MULTIPLE_CHOICE answer. */
    public AttemptAnswer(QuizAttempt attempt, Question question, Set<AnswerOption> selectedOptions, boolean correct) {
        this(attempt, question, (AnswerOption) null, correct);
        this.selectedOptions.addAll(selectedOptions);
    }

    /** SHORT_ANSWER answer. */
    public AttemptAnswer(QuizAttempt attempt, Question question, String textAnswer, boolean correct) {
        this(attempt, question, (AnswerOption) null, correct);
        this.textAnswer = textAnswer;
    }

    public UUID getId() { return id; }
    public QuizAttempt getAttempt() { return attempt; }
    public Question getQuestion() { return question; }
    public AnswerOption getSelectedOption() { return selectedOption; }
    public boolean isCorrect() { return correct; }
    public Set<AnswerOption> getSelectedOptions() { return selectedOptions; }
    public String getTextAnswer() { return textAnswer; }
}
