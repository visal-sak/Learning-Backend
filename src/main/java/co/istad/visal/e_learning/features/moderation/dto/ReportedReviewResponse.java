package co.istad.visal.e_learning.features.moderation.dto;

import co.istad.visal.e_learning.features.moderation.ReportReason;
import co.istad.visal.e_learning.features.moderation.ReportStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One review in the moderation queue with every report filed against it. */
public record ReportedReviewResponse(
        UUID reviewId,
        UUID courseId,
        String courseTitle,
        String authorName,
        String authorAvatarUrl,
        short rating,
        String comment,
        Instant reviewedAt,
        ReportStatus status,
        int reportCount,
        Instant lastReportedAt,
        List<Report> reports
) {
    public record Report(
            UUID id,
            String reporterName,
            ReportReason reason,
            String details,
            Instant createdAt
    ) {
    }
}
