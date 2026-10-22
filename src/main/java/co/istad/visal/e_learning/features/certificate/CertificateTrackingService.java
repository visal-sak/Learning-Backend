package co.istad.visal.e_learning.features.certificate;

import co.istad.visal.e_learning.features.certificate.dto.CertificateTrackingResponse;
import co.istad.visal.e_learning.features.certificate.dto.CertificateTrackingResponse.*;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.enrollment.Enrollment;
import co.istad.visal.e_learning.features.enrollment.EnrollmentRepository;
import co.istad.visal.e_learning.features.enrollment.EnrollmentStatus;
import co.istad.visal.e_learning.features.quiz.Quiz;
import co.istad.visal.e_learning.features.quiz.QuizAttempt;
import co.istad.visal.e_learning.features.quiz.QuizAttemptRepository;
import co.istad.visal.e_learning.features.quiz.QuizRepository;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CurrentUserService;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Certificate tracking for teaching staff. An admin sees every course,
 * an instructor only the courses they teach (same scoping as Assessments).
 */
@Service
@Transactional(readOnly = true)
public class CertificateTrackingService {

    private final CertificateRepository certificates;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final CurrentUserService currentUser;

    public CertificateTrackingService(CertificateRepository certificates, CourseRepository courses,
                                      EnrollmentRepository enrollments, QuizRepository quizzes,
                                      QuizAttemptRepository attempts, CurrentUserService currentUser) {
        this.certificates = certificates;
        this.courses = courses;
        this.enrollments = enrollments;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.currentUser = currentUser;
    }

    public CertificateTrackingResponse overview() {
        User me = currentUser.require();
        boolean admin = currentUser.hasRole(KeycloakRoleEnum.ADMIN);
        List<Course> scope = admin ? courses.findAll() : courses.findByInstructorIdOrderByUpdatedAtDesc(me.getId());
        Set<UUID> courseIds = new HashSet<>();
        scope.forEach(c -> courseIds.add(c.getId()));

        // Issued — admins also see certificates whose course was later deleted (course is null then)
        List<Certificate> issued = (admin ? certificates.findAllByOrderByIssuedAtDesc()
                : certificates.findByCourseIdInOrderByIssuedAtDesc(courseIds));
        Set<String> certifiedKeys = new HashSet<>();
        Map<UUID, Long> certifiedPerCourse = new HashMap<>();
        List<Issued> issuedRows = new ArrayList<>();
        for (Certificate c : issued) {
            UUID userId = c.getUser() == null ? null : c.getUser().getId();
            UUID courseId = c.getCourse() == null ? null : c.getCourse().getId();
            if (userId != null && courseId != null) certifiedKeys.add(userId + ":" + courseId);
            if (courseId != null) certifiedPerCourse.merge(courseId, 1L, Long::sum);
            issuedRows.add(new Issued(c.getSerialNo(), userId, c.getStudentName(),
                    c.getUser() == null ? null : c.getUser().getEmail(),
                    courseId, c.getCourseTitle(), c.getInstructorName(), c.getQuizScore(), c.getIssuedAt()));
        }

        // Finished every lesson but no certificate yet
        List<CourseRow> courseRows = new ArrayList<>();
        List<Awaiting> awaiting = new ArrayList<>();
        long finishedStudents = 0;
        for (Course course : scope) {
            List<Enrollment> list = enrollments.findByCourseIdOrderByEnrolledAtDesc(course.getId());
            Optional<Quiz> quiz = quizzes.findByCourseId(course.getId());
            long enrolled = list.stream().filter(e -> e.getStatus() != EnrollmentStatus.CANCELLED).count();
            long finished = 0;
            long waiting = 0;
            for (Enrollment e : list) {
                if (e.getStatus() != EnrollmentStatus.COMPLETED) continue;
                finished++;
                if (certifiedKeys.contains(e.getUser().getId() + ":" + course.getId())) continue;
                waiting++;
                awaiting.add(awaitingRow(e, course, quiz));
            }
            finishedStudents += finished;
            courseRows.add(new CourseRow(course.getId(), course.getTitle(), course.getInstructor().getDisplayName(),
                    quiz.isPresent(), enrolled, finished, certifiedPerCourse.getOrDefault(course.getId(), 0L), waiting));
        }
        courseRows.sort(Comparator.comparingLong(CourseRow::certified).reversed().thenComparing(CourseRow::courseTitle));
        awaiting.sort(Comparator.comparing(Awaiting::finishedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        Instant monthStart = ZonedDateTime.now(ZoneOffset.UTC).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        long thisMonth = issuedRows.stream().filter(r -> !r.issuedAt().isBefore(monthStart)).count();

        return new CertificateTrackingResponse(
                new Summary(issuedRows.size(), thisMonth, awaiting.size(), finishedStudents),
                courseRows, issuedRows, awaiting);
    }

    private Awaiting awaitingRow(Enrollment e, Course course, Optional<Quiz> quiz) {
        User u = e.getUser();
        Short best = null;
        Short passMark = null;
        int tries = 0;
        String reason = "NOT_CLAIMED";
        if (quiz.isPresent()) {
            passMark = quiz.get().getPassMark();
            List<QuizAttempt> mine = attempts.findByEnrollmentIdAndQuizIdOrderByAttemptNoDesc(e.getId(), quiz.get().getId());
            tries = mine.size();
            best = mine.stream().map(QuizAttempt::getScore).max(Short::compare).orElse(null);
            boolean passed = mine.stream().anyMatch(QuizAttempt::isPassed);
            reason = tries == 0 ? "QUIZ_NOT_TAKEN" : passed ? "NOT_CLAIMED" : "QUIZ_NOT_PASSED";
        }
        return new Awaiting(u.getId(), u.getDisplayName(), u.getEmail(), course.getId(), course.getTitle(),
                e.getCompletedAt(), best, passMark, tries, reason);
    }
}
