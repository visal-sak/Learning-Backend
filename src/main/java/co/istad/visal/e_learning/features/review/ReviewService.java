package co.istad.visal.e_learning.features.review;

import co.istad.visal.e_learning.features.review.dto.RatingSummaryResponse;
import co.istad.visal.e_learning.features.review.dto.ReviewResponse;
import co.istad.visal.e_learning.features.review.dto.SaveReviewRequest;
import co.istad.visal.e_learning.utils.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    /** Public. Anyone browsing a published course sees these. */
    PageResponse<ReviewResponse> findByCourse(UUID courseId, Pageable pageable);

    RatingSummaryResponse summary(UUID courseId);

    /** The caller's own review, or 404 if they have not written one. */
    ReviewResponse findMine(UUID courseId);

    /** Creates or replaces. Requires an enrollment and some progress. */
    ReviewResponse save(UUID courseId, SaveReviewRequest request);

    void deleteMine(UUID courseId);

    /** Admin only — for removing abuse. */
    void deleteAny(UUID reviewId);

    /** Everything written about one instructor's courses. */
    PageResponse<ReviewResponse> findForInstructor(Pageable pageable);
}
