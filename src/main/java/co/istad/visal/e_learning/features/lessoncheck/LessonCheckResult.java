package co.istad.visal.e_learning.features.lessoncheck;

import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/** A student's latest score on a lesson check (one row per student per lesson, overwritten on retry). */
@Entity
@Table(name = "lesson_check_results", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lesson_check_results_user_lesson", columnNames = {"user_id", "lesson_id"})
})
public class LessonCheckResult {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lesson lesson;

    @Column(nullable = false)
    private int correct;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private int attempts;

    /** Passed at least once (stays true after a later lower score). */
    @Column
    private Boolean passed;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonCheckResult() {
    }

    public LessonCheckResult(User user, Lesson lesson) {
        this.user = user;
        this.lesson = lesson;
    }

    public void record(int correct, int total, boolean passedNow) {
        if (passedNow) this.passed = true;
        this.correct = correct;
        this.total = total;
        this.attempts++;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public int getCorrect() { return correct; }
    public int getTotal() { return total; }
    public int getAttempts() { return attempts; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean isPassed() { return Boolean.TRUE.equals(passed); }
}
