package co.istad.visal.e_learning.features.report;

import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.course.CourseStatus;
import co.istad.visal.e_learning.features.enrollment.EnrollmentRepository;
import co.istad.visal.e_learning.features.report.dto.InstructorStatsResponse;
import co.istad.visal.e_learning.features.report.dto.PlatformStatsResponse;
import co.istad.visal.e_learning.features.report.dto.TopCourseResponse;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.features.user.UserRepository;
import co.istad.visal.e_learning.security.CurrentUserService;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import co.istad.visal.e_learning.security.KeycloakUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ReportServiceImpl implements ReportService {

    private static final int MAX_LIMIT = 50;

    private final ReportRepository reportRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUser;
    private final KeycloakUserService keycloakUsers;

    public ReportServiceImpl(ReportRepository reportRepository,
                             CourseRepository courseRepository,
                             EnrollmentRepository enrollmentRepository,
                             UserRepository userRepository,
                             CurrentUserService currentUser,
                             KeycloakUserService keycloakUsers) {
        this.reportRepository = reportRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.keycloakUsers = keycloakUsers;
    }

    @Override
    public PlatformStatsResponse platformStats() {
        return new PlatformStatsResponse(
                countAccounts(),
                countInstructors(),
                courseRepository.countByStatus(CourseStatus.PUBLISHED),
                courseRepository.countByStatus(CourseStatus.DRAFT),
                courseRepository.countByStatus(CourseStatus.PENDING_REVIEW),
                enrollmentRepository.count(),
                reportRepository.countLessons(),
                reportRepository.countQuizAttempts(),
                round(reportRepository.platformAverageRating()));
    }

    /**
     * Keycloak holds every account. The users table only gains a row at first
     * login, so it lags behind. It stands in when Keycloak is unreachable, so
     * one slow service cannot take the whole dashboard down.
     */
    private long countAccounts() {
        return fromKeycloak(() -> keycloakUsers.countUsers(null), userRepository::count);
    }

    /** Whoever holds the role, which includes instructors who have no course yet. */
    private long countInstructors() {
        return fromKeycloak(() -> keycloakUsers.countUsersWithRole(KeycloakRoleEnum.INSTRUCTOR), reportRepository::countInstructors);
    }

    private long fromKeycloak(LongSupplier count, LongSupplier localFallback) {
        try {
            return count.getAsLong();
        } catch (RuntimeException ex) {
            log.warn("Could not count from Keycloak, using the local count: {}", ex.getMessage());
            return localFallback.getAsLong();
        }
    }

    @Override
    public InstructorStatsResponse instructorStats() {
        User me = currentUser.require();
        UUID id = me.getId();

        return new InstructorStatsResponse(
                reportRepository.countCoursesByInstructor(id),
                reportRepository.countPublishedByInstructor(id),
                reportRepository.countStudentsByInstructor(id),
                reportRepository.countLessonsByInstructor(id),
                round(reportRepository.averageRatingByInstructor(id)),
                reportRepository.countReviewsByInstructor(id));
    }

    @Override
    public List<TopCourseResponse> topCourses(int limit) {
        return reportRepository.topByEnrollment(page(limit)).stream()
                .map(this::toTopCourse)
                .toList();
    }

    @Override
    public List<TopCourseResponse> topRatedCourses(int limit) {
        return reportRepository.topByRating(page(limit)).stream()
                .map(this::toTopCourse)
                .toList();
    }

    // ---------------------------------------------------------------

    /**
     * Clamped. A caller asking for limit=100000 should get 50, not a query
     * that walks the whole table.
     */
    private Pageable page(int limit) {
        int safe = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return PageRequest.of(0, safe);
    }

    private TopCourseResponse toTopCourse(Object[] row) {
        return new TopCourseResponse(
                (UUID) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                (Long) row[5],
                round(((Number) row[6]).doubleValue()),
                (Long) row[7]);
    }

    /** AVG returns 4.333333333333333 — more precision than the data supports. */
    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
