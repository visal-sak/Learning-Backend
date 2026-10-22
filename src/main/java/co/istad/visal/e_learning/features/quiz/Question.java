package co.istad.visal.e_learning.features.quiz;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    /** Shown after submission only. Keep it out of the student DTO. */
    @Column(columnDefinition = "text")
    private String explanation;

    @Column(nullable = false)
    private short points = 1;

    @Column(nullable = false)
    private int position;

    /** Null on rows created before question types existed — read through getType(). */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private QuestionType type;

    /**
     * SHORT_ANSWER only: accepted answers, one per line. As sensitive as
     * AnswerOption.isCorrect — never put it in a student DTO.
     */
    @Column(name = "answer_key", columnDefinition = "text")
    private String answerKey;

    @OrderBy("position ASC")
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerOption> options = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Question() {
    }

    public Question(Quiz quiz, String text, int position) {
        this.quiz = quiz;
        this.text = text;
        this.position = position;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Server-side only. Nothing that returns this value may reach a student mid-attempt. */
    public AnswerOption correctOption() {
        return options.stream()
                .filter(AnswerOption::isCorrect)
                .findFirst()
                .orElse(null);
    }

    /** Server-side only. Every option marked correct (MULTIPLE_CHOICE has several). */
    public List<AnswerOption> correctOptions() {
        return options.stream().filter(AnswerOption::isCorrect).toList();
    }

    /** SHORT_ANSWER: the accepted answers, normalized for comparison. */
    public List<String> acceptedAnswers() {
        if (answerKey == null) return List.of();
        return Arrays.stream(answerKey.split("\\R"))
                .map(Question::normalize)
                .filter(a -> !a.isEmpty())
                .toList();
    }

    /** Trim, collapse inner whitespace, ignore case — "  New  York " matches "new york". */
    public static String normalize(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public UUID getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public short getPoints() { return points; }
    public void setPoints(short points) { this.points = points; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public List<AnswerOption> getOptions() { return options; }

    public QuestionType getType() { return type == null ? QuestionType.SINGLE_CHOICE : type; }
    public void setType(QuestionType type) { this.type = type; }

    public String getAnswerKey() { return answerKey; }
    public void setAnswerKey(String answerKey) { this.answerKey = answerKey; }
}
