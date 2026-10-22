package co.istad.visal.e_learning.features.review;

import co.istad.visal.e_learning.features.enrollment.Enrollment;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Attached to the enrollment, not the user.
 *
 * Same reasoning as quiz attempts: it makes "only enrolled students can
 * review" a structural fact rather than a check someone has to remember. It
 * also means one review per person per course falls out of a single unique
 * constraint, with no composite key needed.
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uq_reviews_enrollment", columnNames = "enrollment_id")
})
public class Review {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, updatable = false)
    private Enrollment enrollment;

    @Column(nullable = false)
    private short rating;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Review() {
    }

    public Review(Enrollment enrollment, short rating, String comment) {
        this.enrollment = enrollment;
        this.rating = rating;
        this.comment = comment;
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
    public Enrollment getEnrollment() { return enrollment; }
    public short getRating() { return rating; }
    public void setRating(short rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
