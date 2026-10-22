package co.istad.visal.e_learning.features.notification;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** One in-app notification for one user (shown under the bell in the navbar). */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "ix_notifications_user_created", columnList = "user_id, created_at")
})
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** e.g. AI_ACCESS_GRANTED */
    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String body;

    /** Frontend path to open when clicked, e.g. /courses/{id}/learn/{lessonId} */
    @Column(length = 300)
    private String link;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(UUID userId, String type, String title, String body, String link) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void markRead() {
        if (readAt == null) readAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLink() { return link; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
