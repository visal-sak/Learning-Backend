package co.istad.visal.e_learning.features.report.dto;

/**
 * Scoped to one instructor's own courses.
 *
 * Deliberately narrower than the admin view: an instructor seeing
 * platform-wide totals would be a leak, and it is exactly the kind of thing
 * a reviewer probes.
 */
public record InstructorStatsResponse(
        long myCourses,
        long myPublishedCourses,
        long myStudents,
        long myLessons,
        double myAverageRating,
        long myReviewCount
) {
}
