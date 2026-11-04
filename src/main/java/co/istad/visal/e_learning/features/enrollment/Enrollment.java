package co.istad.visal.e_learning.features.enrollment;

import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * The student half of the permission model. One row means this user may
 * consume this course — every gated read in the system asks whether it exists.
 */
@Entity
@Table(name = "enrollments", indexes = @Index(name = "idx_enrollments_course_id", columnList = "course_id"), uniqueConstraints = {
        @UniqueConstraint(name = "uq_enrollments_user_course", columnNames = {"user_id", "course_id"})
})
public class Enrollment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    private Course course;

    // Stored as varchar. See the note in Course — a custom Postgres type
    // needs something to create it first, and Hibernate will not.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    protected Enrollment() {
    }

    public Enrollment(User user, Course course) {
        this.user = user;
        this.course = course;
    }

    @PrePersist
    void onCreate() {
        enrolledAt = Instant.now();
        lastAccessedAt = enrolledAt;
    }

    public boolean isActive() {
        return status == EnrollmentStatus.ACTIVE || status == EnrollmentStatus.COMPLETED;
    }

    public void touch() {
        lastAccessedAt = Instant.now();
    }

    public void markCompleted() {
        status = EnrollmentStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public void reopen() {
        status = EnrollmentStatus.ACTIVE;
        completedAt = null;
    }

    public void cancel() {
        status = EnrollmentStatus.CANCELLED;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Course getCourse() { return course; }
    public EnrollmentStatus getStatus() { return status; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getLastAccessedAt() { return lastAccessedAt; }
}