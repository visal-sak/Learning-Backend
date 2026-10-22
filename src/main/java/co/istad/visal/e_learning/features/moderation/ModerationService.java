package co.istad.visal.e_learning.features.moderation;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.moderation.dto.ReportReviewRequest;
import co.istad.visal.e_learning.features.moderation.dto.ReportedReviewResponse;
import co.istad.visal.e_learning.features.review.Review;
import co.istad.visal.e_learning.features.review.ReviewRepository;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CurrentUserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Students report; admins decide. An instructor never moderates reviews of
 * their own course — that would let them remove the bad ones.
 */
@Service
@Transactional(readOnly = true)
public class ModerationService {

    private final ReviewReportRepository reports;
    private final ReviewRepository reviews;
    private final CurrentUserService currentUser;

    public ModerationService(ReviewReportRepository reports, ReviewRepository reviews, CurrentUserService currentUser) {
        this.reports = reports;
        this.reviews = reviews;
        this.currentUser = currentUser;
    }

    @Transactional
    public void report(UUID reviewId, ReportReviewRequest request) {
        User reporter = currentUser.require();
        Review review = reviews.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("No review with id " + reviewId));

        if (review.getEnrollment().getUser().getId().equals(reporter.getId())) {
            throw new ConflictException("You cannot report your own review");
        }
        if (reports.existsByReviewIdAndReporterId(reviewId, reporter.getId())) {
            throw new ConflictException("You have already reported this review");
        }
        String details = request.details() == null || request.details().isBlank() ? null : request.details().trim();
        try {
            reports.saveAndFlush(new ReviewReport(review, reporter, request.reason(), details));
        } catch (DataIntegrityViolationException doubleClick) {
            throw new ConflictException("You have already reported this review");
        }
    }

    /** The queue: reports grouped by review, most-reported first, then most recent. */
    public List<ReportedReviewResponse> queue(ReportStatus status) {
        Map<UUID, List<ReviewReport>> byReview = new LinkedHashMap<>();
        for (ReviewReport r : reports.findWithReviewByStatus(status)) {
            byReview.computeIfAbsent(r.getReview().getId(), k -> new ArrayList<>()).add(r);
        }
        return byReview.values().stream()
                .map(list -> toResponse(list, status))
                .sorted(Comparator.comparingInt(ReportedReviewResponse::reportCount).reversed()
                        .thenComparing(ReportedReviewResponse::lastReportedAt, Comparator.reverseOrder()))
                .toList();
    }

    public long openCount() {
        return reports.countByStatus(ReportStatus.OPEN);
    }

    /** Keep the review: close its open reports. */
    @Transactional
    public void dismiss(UUID reviewId) {
        User admin = currentUser.require();
        List<ReviewReport> open = reports.findByReviewIdAndStatus(reviewId, ReportStatus.OPEN);
        if (open.isEmpty()) throw new NotFoundException("No open reports for review " + reviewId);
        open.forEach(r -> r.dismiss(admin));
    }

    /** Take the review down. Its reports go with it (ON DELETE CASCADE). */
    @Transactional
    public void remove(UUID reviewId) {
        Review review = reviews.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("No review with id " + reviewId));
        reviews.delete(review);
    }

    private ReportedReviewResponse toResponse(List<ReviewReport> list, ReportStatus status) {
        Review review = list.get(0).getReview();
        User author = review.getEnrollment().getUser();
        return new ReportedReviewResponse(
                review.getId(),
                review.getEnrollment().getCourse().getId(),
                review.getEnrollment().getCourse().getTitle(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                review.getRating(),
                review.getComment(),
                review.getUpdatedAt(),
                status,
                list.size(),
                list.stream().map(ReviewReport::getCreatedAt).max(Comparator.naturalOrder()).orElse(null),
                list.stream()
                        .map(r -> new ReportedReviewResponse.Report(
                                r.getId(),
                                r.getReporter().getDisplayName(),
                                r.getReason(),
                                r.getDetails(),
                                r.getCreatedAt()))
                        .toList());
    }
}
