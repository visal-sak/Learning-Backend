package co.istad.visal.e_learning.features.report;

import co.istad.visal.e_learning.features.report.dto.InstructorStatsResponse;
import co.istad.visal.e_learning.features.report.dto.PlatformStatsResponse;
import co.istad.visal.e_learning.features.report.dto.TopCourseResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Three audiences, three scopes. The admin sees the platform, the instructor
 * sees their own courses, and anyone browsing sees the leaderboards.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/platform")
    @PreAuthorize("hasAuthority('ADMIN')")
    public PlatformStatsResponse platform() {
        return service.platformStats();
    }

    /** Scoped to the caller's own courses by the service, not by a parameter. */
    @GetMapping("/my-teaching")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public InstructorStatsResponse myTeaching() {
        return service.instructorStats();
    }

    @GetMapping("/top-courses")
    public List<TopCourseResponse> topCourses(@RequestParam(defaultValue = "5") int limit) {
        return service.topCourses(limit);
    }

    @GetMapping("/top-rated")
    public List<TopCourseResponse> topRated(@RequestParam(defaultValue = "5") int limit) {
        return service.topRatedCourses(limit);
    }
}
