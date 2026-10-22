package co.istad.visal.e_learning.features.lessoncheck;

import co.istad.visal.e_learning.features.content.Lesson;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/**
 * A short "check your understanding" quiz at the end of one lesson.
 * Separate from the course's final quiz: no attempt limit, no pass mark, no certificate.
 * Questions (with their answer keys) are stored as JSON and never sent to students before they answer.
 */
@Entity
@Table(name = "lesson_checks")
public class LessonCheck {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lesson lesson;

    /** JSON list of CheckQuestion. */
    @Column(nullable = false, columnDefinition = "text")
    private String questions;

    /** % needed to pass (null = any submission counts). */
    @Column(name = "pass_mark")
    private Short passMark;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonCheck() {
    }

    public LessonCheck(Lesson lesson) {
        this.lesson = lesson;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Lesson getLesson() { return lesson; }
    public String getQuestions() { return questions; }
    public void setQuestions(String questions) { this.questions = questions; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Short getPassMark() { return passMark; }
    public void setPassMark(Short passMark) { this.passMark = passMark; }
}
