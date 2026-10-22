package co.istad.visal.e_learning.features.bookmark;

import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/** A lesson a student saved to come back to. One per student per lesson. */
@Entity
@Table(name = "lesson_bookmarks", uniqueConstraints = {
        @UniqueConstraint(name = "uq_lesson_bookmarks_user_lesson", columnNames = {"user_id", "lesson_id"})
})
public class Bookmark {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /** Deleting the lesson removes its bookmarks with it. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lesson lesson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Bookmark() {
    }

    public Bookmark(User user, Lesson lesson) {
        this.user = user;
        this.lesson = lesson;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Lesson getLesson() { return lesson; }
    public Instant getCreatedAt() { return createdAt; }
}
