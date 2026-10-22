package co.istad.visal.e_learning.security;

import co.istad.visal.e_learning.expection.ForbiddenException;
import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.course.CourseStatus;
import co.istad.visal.e_learning.features.enrollment.Enrollment;
import co.istad.visal.e_learning.features.enrollment.EnrollmentRepository;
import co.istad.visal.e_learning.features.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Layer two of the authorization model.
 *
 * Keycloak answers "is this user an instructor". It cannot answer "is this
 * THEIR course" or "is this student enrolled" — only these tables can. Every
 * ServiceImpl injects this class rather than writing ownership checks inline,
 * so the rules live in one file and change in one place.
 */
@Service
public class CourseAccessService {

    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUser;

    public CourseAccessService(EnrollmentRepository enrollmentRepository, CurrentUserService currentUser) {
        this.enrollmentRepository = enrollmentRepository;
        this.currentUser = currentUser;
    }

    // ---------------------------------------------------------------
    //  Instructor side — ownership
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public boolean canEdit(Course course) {
        if (currentUser.hasRole(KeycloakRoleEnum.ADMIN)) {
            return true;
        }
        return currentUser.current()
                .map(user -> isOwner(course, user) && course.getStatus() != CourseStatus.PENDING_REVIEW)
                .orElse(false);
    }

    public void requireCanEdit(Course course) {
        if (!canEdit(course)) {
            throw new ForbiddenException("You cannot edit course " + course.getId());
        }
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Course course, User user) {
        return course.getInstructor().getId().equals(user.getId());
    }

    // ---------------------------------------------------------------
    //  Student side — enrollment
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public boolean isEnrolled(Course course) {
        return activeEnrollment(course).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<Enrollment> activeEnrollment(Course course) {
        return currentUser.current()
                .flatMap(user -> enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId()))
                .filter(Enrollment::isActive);
    }

    public Enrollment requireEnrollment(Course course) {
        return activeEnrollment(course)
                .orElseThrow(() -> new ForbiddenException("You are not enrolled in course " + course.getId()));
    }

    // ---------------------------------------------------------------
    //  Content gate — the check that runs on every video request
    // ---------------------------------------------------------------

    /**
     * Order matters. The preview check comes first so the public catalog never
     * touches the enrollment table.
     */
    @Transactional(readOnly = true)
    public boolean canViewContent(Lesson lesson) {
        if (lesson.isPreview()) {
            return true;
        }
        if (currentUser.hasRole(KeycloakRoleEnum.ADMIN)) {
            return true;
        }

        Course course = lesson.getSection().getCourse();

        return currentUser.current()
                .map(user -> isOwner(course, user) || isEnrolled(course))
                .orElse(false);
    }

    public void requireCanViewContent(Lesson lesson) {
        if (!canViewContent(lesson)) {
            throw new ForbiddenException("Lesson " + lesson.getId() + " is not available to you");
        }
    }

    // ---------------------------------------------------------------
    //  Course visibility — drafts are private to their author
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public boolean canView(Course course) {
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            return true;
        }
        if (currentUser.hasRole(KeycloakRoleEnum.ADMIN)) {
            return true;
        }
        return currentUser.current().map(user -> isOwner(course, user)).orElse(false);
    }

    public void requireCanView(Course course) {
        if (!canView(course)) {
            throw new ForbiddenException("Course " + course.getId() + " is not visible to you");
        }
    }

    /** Instructors see attempts and progress for their own courses only. */
    public void requireCanViewStudentData(Course course) {
        if (currentUser.hasRole(KeycloakRoleEnum.ADMIN)) {
            return;
        }
        boolean owner = currentUser.current().map(user -> isOwner(course, user)).orElse(false);
        if (!owner) {
            throw new ForbiddenException("Student data for course " + course.getId() + " is not yours to read");
        }
    }
}