package co.istad.visal.e_learning.features.note;

import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/**
 * A student's private note on a lesson, optionally pinned to a moment in the
 * lesson video (timestampSeconds), so the note can jump the player back there.
 */
@Entity
@Table(name = "lesson_notes", indexes = {
        @Index(name = "ix_lesson_notes_user_lesson", columnList = "user_id, lesson_id")
})
public class LessonNote {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /** Deleting the lesson removes its notes with it. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lesson lesson;

    /** Seconds into the lesson video; null for a note that isn't tied to a moment. */
    @Column(name = "timestamp_seconds")
    private Integer timestampSeconds;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonNote() {
    }

    public LessonNote(User user, Lesson lesson, Integer timestampSeconds, String body) {
        this.user = user;
        this.lesson = lesson;
        this.timestampSeconds = timestampSeconds;
        this.body = body;
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

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Lesson getLesson() { return lesson; }
    public Integer getTimestampSeconds() { return timestampSeconds; }
    public void setTimestampSeconds(Integer timestampSeconds) { this.timestampSeconds = timestampSeconds; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
