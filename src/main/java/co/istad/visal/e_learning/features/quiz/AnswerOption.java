package co.istad.visal.e_learning.features.quiz;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * isCorrect is the single most sensitive column in the schema. If it ever
 * reaches the browser before submission the quiz is decorative — dev tools,
 * read the answers, score 100%.
 */
@Entity
@Table(name = "answer_options")
public class AnswerOption {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private int position;

    protected AnswerOption() {
    }

    public AnswerOption(Question question, String text, boolean correct, int position) {
        this.question = question;
        this.text = text;
        this.correct = correct;
        this.position = position;
    }

    public UUID getId() { return id; }
    public Question getQuestion() { return question; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
