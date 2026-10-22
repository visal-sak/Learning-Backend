package co.istad.visal.e_learning.features.ai;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Extra Bunny AI help for one student on one lesson, given by an admin or the course's instructor:
 * more tutor questions and/or more quiz hints per attempt. Several grants add up.
 */
@Entity
@Table(name = "ai_access_grants", indexes = {
        @Index(name = "ix_ai_access_user_lesson", columnList = "user_id, lesson_id"),
        @Index(name = "ix_ai_access_course", columnList = "course_id")
})
public class AiAccessGrant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "extra_questions", nullable = false)
    private int extraQuestions;

    @Column(name = "extra_hints", nullable = false)
    private int extraHints;

    @Column(length = 500)
    private String note;

    @Column(name = "granted_by", nullable = false)
    private UUID grantedBy;

    @Column(name = "granted_by_name", length = 200)
    private String grantedByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiAccessGrant() {
    }

    public AiAccessGrant(UUID userId, UUID courseId, UUID lessonId, int extraQuestions, int extraHints, String note,
                         UUID grantedBy, String grantedByName) {
        this.userId = userId;
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.extraQuestions = extraQuestions;
        this.extraHints = extraHints;
        this.note = note;
        this.grantedBy = grantedBy;
        this.grantedByName = grantedByName;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCourseId() { return courseId; }
    public UUID getLessonId() { return lessonId; }
    public int getExtraQuestions() { return extraQuestions; }
    public int getExtraHints() { return extraHints; }
    public String getNote() { return note; }
    public UUID getGrantedBy() { return grantedBy; }
    public String getGrantedByName() { return grantedByName; }
    public Instant getCreatedAt() { return createdAt; }
}
