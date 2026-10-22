package co.istad.visal.e_learning.features.review;

import co.istad.visal.e_learning.features.review.dto.RatingSummaryResponse;
import co.istad.visal.e_learning.features.review.dto.ReviewResponse;
import co.istad.visal.e_learning.features.review.dto.SaveReviewRequest;
import co.istad.visal.e_learning.utils.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    // ---------- public ----------

    @GetMapping("/courses/{courseId}/reviews")
    public PageResponse<ReviewResponse> byCourse(
            @PathVariable UUID courseId,
            @PageableDefault(size = 10) Pageable pageable) {

        return service.findByCourse(courseId, pageable);
    }

    @GetMapping("/courses/{courseId}/reviews/summary")
    public RatingSummaryResponse summary(@PathVariable UUID courseId) {
        return service.summary(courseId);
    }

    // ---------- student, own review ----------

    /**
     * No @PreAuthorize. "Enrolled in this course" is not a role, so the check
     * belongs in the service — same reasoning as the lesson content endpoint.
     */
    @GetMapping("/courses/{courseId}/reviews/mine")
    public ReviewResponse mine(@PathVariable UUID courseId) {
        return service.findMine(courseId);
    }

    /** Creates on the first call, edits on every one after. */
    @PutMapping("/courses/{courseId}/reviews/mine")
    public ReviewResponse save(@PathVariable UUID courseId,
                               @Valid @RequestBody SaveReviewRequest request) {
        return service.save(courseId, request);
    }

    @DeleteMapping("/courses/{courseId}/reviews/mine")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMine(@PathVariable UUID courseId) {
        service.deleteMine(courseId);
    }

    // ---------- instructor ----------

    @GetMapping("/my/reviews")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public PageResponse<ReviewResponse> forInstructor(@PageableDefault(size = 20) Pageable pageable) {
        return service.findForInstructor(pageable);
    }

    // ---------- admin ----------

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAny(@PathVariable UUID reviewId) {
        service.deleteAny(reviewId);
    }
}
