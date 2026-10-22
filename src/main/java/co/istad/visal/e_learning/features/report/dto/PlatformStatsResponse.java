package co.istad.visal.e_learning.features.report.dto;

/** The admin dashboard. Every figure is a count over rows that already exist. */
public record PlatformStatsResponse(
        long totalUsers,
        long totalInstructors,
        long publishedCourses,
        long draftCourses,
        long pendingReview,
        long totalEnrollments,
        long totalLessons,
        long quizAttempts,
        double averageRating
) {
}
