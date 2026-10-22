package co.istad.visal.e_learning.features.moderation;

import co.istad.visal.e_learning.features.review.Review;
import co.istad.visal.e_learning.features.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/** A student flagging a review. One report per reporter per review. */
@Entity
@Table(name = "review_reports", uniqueConstraints = {
        @UniqueConstraint(name = "uq_review_reports_review_reporter", columnNames = {"review_id", "reporter_id"})
})
public class ReviewReport {

    @Id
    @GeneratedValue
    private UUID id;

    /** Removing the review (by its author or an admin) removes its reports too. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportReason reason;

    @Column(length = 500)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User resolvedBy;

    protected ReviewReport() {
    }

    public ReviewReport(Review review, User reporter, ReportReason reason, String details) {
        this.review = review;
        this.reporter = reporter;
        this.reason = reason;
        this.details = details;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void dismiss(User admin) {
        status = ReportStatus.DISMISSED;
        resolvedAt = Instant.now();
        resolvedBy = admin;
    }

    public UUID getId() { return id; }
    public Review getReview() { return review; }
    public User getReporter() { return reporter; }
    public ReportReason getReason() { return reason; }
    public String getDetails() { return details; }
    public ReportStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public User getResolvedBy() { return resolvedBy; }
}
