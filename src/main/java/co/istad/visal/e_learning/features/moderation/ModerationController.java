package co.istad.visal.e_learning.features.moderation;

import co.istad.visal.e_learning.features.moderation.dto.ReportReviewRequest;
import co.istad.visal.e_learning.features.moderation.dto.ReportedReviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ModerationController {

    private final ModerationService service;

    public ModerationController(ModerationService service) {
        this.service = service;
    }

    // ---------- any signed-in user ----------

    @PostMapping("/reviews/{reviewId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public void report(@PathVariable UUID reviewId, @Valid @RequestBody ReportReviewRequest request) {
        service.report(reviewId, request);
    }

    // ---------- admin queue ----------

    @GetMapping("/moderation/reviews")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<ReportedReviewResponse> queue(@RequestParam(defaultValue = "OPEN") ReportStatus status) {
        return service.queue(status);
    }

    @GetMapping("/moderation/summary")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Map<String, Long> summary() {
        return Map.of("openReviewReports", service.openCount());
    }

    @PostMapping("/moderation/reviews/{reviewId}/dismiss")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismiss(@PathVariable UUID reviewId) {
        service.dismiss(reviewId);
    }

    @DeleteMapping("/moderation/reviews/{reviewId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID reviewId) {
        service.remove(reviewId);
    }
}
