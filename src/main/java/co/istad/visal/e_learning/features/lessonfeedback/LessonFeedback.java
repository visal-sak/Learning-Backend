package co.istad.visal.e_learning.features.lessonfeedback;

import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/**
 * A student's verdict on one lesson. Course reviews say whether a course is good;
 * this says which lesson needs work. One row per student per lesson, so changing
 * their mind updates it rather than adding another.
 */
@Entity
@Table(name = "lesson_feedback", uniqueConstraints = {
        @UniqueConstraint(name = "uq_lesson_feedback_user_lesson", columnNames = {"user_id", "lesson_id"})
}, indexes = @Index(name = "idx_lesson_feedback_lesson_id", columnList = "lesson_id"))
public class LessonFeedback {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /** Deleting the lesson removes its feedback with it. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lesson lesson;

    @Column(nullable = false)
    private boolean helpful;

    @Column(length = 500)
    private String comment;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonFeedback() {
    }

    public LessonFeedback(User user, Lesson lesson) {
        this.user = user;
        this.lesson = lesson;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public void update(boolean helpful, String comment) {
        this.helpful = helpful;
        this.comment = comment;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Lesson getLesson() { return lesson; }
    public boolean isHelpful() { return helpful; }
    public String getComment() { return comment; }
    public Instant getUpdatedAt() { return updatedAt; }
}
