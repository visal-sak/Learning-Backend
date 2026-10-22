package co.istad.visal.e_learning.features.enrollment;

import co.istad.visal.e_learning.features.content.Lesson;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per completed lesson. The course percentage is a count over these
 * rows, never a stored column — a stored figure drifts the moment an
 * instructor adds a lesson.
 */
@Entity
@Table(name = "lesson_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uq_progress_enrollment_lesson", columnNames = {"enrollment_id", "lesson_id"})
})
public class LessonProgress {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, updatable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, updatable = false)
    private Lesson lesson;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private Instant completedAt;

    protected LessonProgress() {
    }

    public LessonProgress(Enrollment enrollment, Lesson lesson) {
        this.enrollment = enrollment;
        this.lesson = lesson;
    }

    @PrePersist
    void onCreate() {
        completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Enrollment getEnrollment() { return enrollment; }
    public Lesson getLesson() { return lesson; }
    public Instant getCompletedAt() { return completedAt; }
}
