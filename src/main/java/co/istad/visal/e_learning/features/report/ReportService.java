package co.istad.visal.e_learning.features.report;

import co.istad.visal.e_learning.features.report.dto.InstructorStatsResponse;
import co.istad.visal.e_learning.features.report.dto.PlatformStatsResponse;
import co.istad.visal.e_learning.features.report.dto.TopCourseResponse;

import java.util.List;

/**
 * No entity and no repository of its own. Every figure here is a query over
 * tables that already exist, which is why this module adds nothing to the
 * schema.
 */
public interface ReportService {

    PlatformStatsResponse platformStats();

    InstructorStatsResponse instructorStats();

    /** Published courses ranked by enrolment. Public. */
    List<TopCourseResponse> topCourses(int limit);

    /** Published courses ranked by average rating, with a minimum review count. */
    List<TopCourseResponse> topRatedCourses(int limit);
}
