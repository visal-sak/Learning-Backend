package co.istad.visal.e_learning.features.report.dto;

import java.util.UUID;

public record TopCourseResponse(
        UUID courseId,
        String title,
        String thumbnailUrl,
        String instructorName,
        String categoryName,
        long enrollmentCount,
        double averageRating,
        long reviewCount
) {
}
