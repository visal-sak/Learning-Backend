package co.istad.visal.e_learning.features.quiz;

import co.istad.visal.e_learning.features.enrollment.Enrollment;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Attached to the enrollment rather than the user. That makes "you must be
 * enrolled to have an attempt" a structural fact instead of a rule someone
 * has to remember to check.
 */
@Entity
@Table(name = "quiz_attempts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_attempts_no", columnNames = {"enrollment_id", "quiz_id", "attempt_no"})
})
public class QuizAttempt {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, updatable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false, updatable = false)
    private Quiz quiz;

    @Column(name = "attempt_no", nullable = false)
    private short attemptNo = 1;

    /** Written by the server. There is no setter that takes a client value. */
    @Column(nullable = false)
    private short score;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttemptAnswer> answers = new ArrayList<>();

    protected QuizAttempt() {
    }

    public QuizAttempt(Enrollment enrollment, Quiz quiz, short attemptNo) {
        this.enrollment = enrollment;
        this.quiz = quiz;
        this.attemptNo = attemptNo;
        this.startedAt = Instant.now();
    }

    public void grade(short score, boolean passed) {
        this.score = score;
        this.passed = passed;
        this.submittedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public Enrollment getEnrollment() { return enrollment; }
    public Quiz getQuiz() { return quiz; }
    public short getAttemptNo() { return attemptNo; }
    public short getScore() { return score; }
    public boolean isPassed() { return passed; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public List<AttemptAnswer> getAnswers() { return answers; }
}
