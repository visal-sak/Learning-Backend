package co.istad.visal.e_learning.features.enrollment;

import co.istad.visal.e_learning.features.certificate.CertificateService;
import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.ForbiddenException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.course.CourseStatus;
import co.istad.visal.e_learning.features.enrollment.dto.EnrollmentResponse;
import co.istad.visal.e_learning.features.enrollment.dto.ProgressResponse;
import co.istad.visal.e_learning.features.enrollment.dto.StudentProgressResponse;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository repository;
    private final LessonProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;
    private final CertificateService certificates;
    private final co.istad.visal.e_learning.features.lessoncheck.LessonCheckResultRepository checkResults;

    public EnrollmentServiceImpl(EnrollmentRepository repository,
                                 LessonProgressRepository progressRepository,
                                 CourseRepository courseRepository,
                                 LessonRepository lessonRepository,
                                 CourseAccessService access,
                                 CurrentUserService currentUser,
                                 CertificateService certificates,
                                 co.istad.visal.e_learning.features.lessoncheck.LessonCheckResultRepository checkResults) {
        this.checkResults = checkResults;
        this.repository = repository;
        this.progressRepository = progressRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.access = access;
        this.currentUser = currentUser;
        this.certificates = certificates;
    }

    // ---------------------------------------------------------------
    //  Enroll
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public EnrollmentResponse enroll(UUID courseId) {
        User user = currentUser.require();
        Course course = loadCourse(courseId);

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ConflictException("Only published courses accept enrollments");
        }
        if (access.isOwner(course, user)) {
            throw new ConflictException("You cannot enroll in a course you teach");
        }
        if (repository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new ConflictException("You are already enrolled in this course");
        }

        try {
            Enrollment enrollment = repository.save(new Enrollment(user, course));
            return toResponse(enrollment);
        } catch (DataIntegrityViolationException ex) {
            // Two clicks landing at once. The unique index caught the second.
            throw new ConflictException("You are already enrolled in this course");
        }
    }

    // ---------------------------------------------------------------
    //  My learning
    // ---------------------------------------------------------------

    @Override
    public List<EnrollmentResponse> findMine() {
        User user = currentUser.require();
        return repository.findByUserIdOrderByLastAccessedAtDesc(user.getId()).stream()
                .filter(Enrollment::isActive)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EnrollmentResponse findMineByCourse(UUID courseId) {
        User user = currentUser.require();
        Enrollment enrollment = repository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new NotFoundException("You are not enrolled in this course"));
        return toResponse(enrollment);
    }

    // ---------------------------------------------------------------
    //  Progress
    // ---------------------------------------------------------------

    /**
     * The enrollment is derived from the token and the lesson's course. It is
     * never read from the request body — that is how one student ends up
     * writing progress onto another student's enrollment.
     */
    @Override
    @Transactional
    public ProgressResponse markLessonComplete(UUID lessonId) {
        Lesson lesson = lessonRepository.findWithCourse(lessonId)
                .orElseThrow(() -> new NotFoundException("No lesson with id " + lessonId));

        Course course = lesson.getSection().getCourse();
        Enrollment enrollment = access.requireEnrollment(course);

        // A quiz item is completed by submitting it (or passing it, if the course requires that)
        if (lesson.isQuiz() && lesson.getQuestionCount() > 0) {
            var result = checkResults.findByUserIdAndLessonId(enrollment.getUser().getId(), lessonId)
                    .orElseThrow(() -> new co.istad.visal.e_learning.expection.ConflictException("Submit the quiz to complete this item"));
            if (course.isRequireQuizPass() && !result.isPassed()) {
                throw new co.istad.visal.e_learning.expection.ConflictException("Pass the quiz to continue");
            }
        }

        if (!progressRepository.existsByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)) {
            try {
                progressRepository.save(new LessonProgress(enrollment, lesson));
            } catch (DataIntegrityViolationException ignored) {
                // Double click. The unique index already stored it once.
            }
        }

        enrollment.touch();

        int total = (int) lessonRepository.countByCourse(course.getId());
        int done = (int) progressRepository.countByEnrollmentId(enrollment.getId());

        String serial = null;
        if (total > 0 && done >= total) {
            enrollment.markCompleted();
            // Last lesson done: issue the certificate now if the quiz (if any) is already passed
            serial = certificates.issueIfEligible(enrollment).map(c -> c.serialNo()).orElse(null);
        }

        return new ProgressResponse(
                enrollment.getId(), lessonId, done, total, percent(done, total), done >= total && total > 0, serial);
    }

    @Override
    @Transactional
    public ProgressResponse unmarkLessonComplete(UUID lessonId) {
        Lesson lesson = lessonRepository.findWithCourse(lessonId)
                .orElseThrow(() -> new NotFoundException("No lesson with id " + lessonId));

        Course course = lesson.getSection().getCourse();
        Enrollment enrollment = access.requireEnrollment(course);

        progressRepository.deleteByEnrollmentIdAndLessonId(enrollment.getId(), lessonId);
        enrollment.reopen();

        int total = (int) lessonRepository.countByCourse(course.getId());
        int done = (int) progressRepository.countByEnrollmentId(enrollment.getId());

        return new ProgressResponse(enrollment.getId(), lessonId, done, total, percent(done, total), false, null);
    }

    // ---------------------------------------------------------------
    //  Instructor and admin views
    // ---------------------------------------------------------------

    @Override
    public List<StudentProgressResponse> findStudents(UUID courseId) {
        Course course = loadCourse(courseId);
        access.requireCanViewStudentData(course);

        int total = (int) lessonRepository.countByCourse(courseId);

        return repository.findByCourseIdOrderByEnrolledAtDesc(courseId).stream()
                .map(e -> {
                    int done = (int) progressRepository.countByEnrollmentId(e.getId());
                    return new StudentProgressResponse(
                            e.getUser().getId(),
                            e.getUser().getDisplayName(),
                            e.getUser().getAvatarUrl(),
                            done,
                            total,
                            percent(done, total),
                            e.getEnrolledAt(),
                            e.getLastAccessedAt());
                })
                .toList();
    }

    @Override
    @Transactional
    public void unenroll(UUID courseId) {
        User user = currentUser.require();
        Enrollment enrollment = repository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new NotFoundException("You are not enrolled in this course"));

        // Progress rows reference this enrollment. The foreign key is not
        // cascading — the schema.sql version was, but Hibernate built these
        // tables from the entities, which say nothing about cascade.
        progressRepository.deleteByEnrollmentId(enrollment.getId());
        repository.delete(enrollment);
    }

    @Override
    @Transactional
    public void removeStudent(UUID courseId, UUID userId) {
        if (!currentUser.hasRole(KeycloakRoleEnum.ADMIN)) {
            throw new ForbiddenException("Only an admin can remove a student from a course");
        }

        Enrollment enrollment = repository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new NotFoundException("That student is not enrolled in this course"));

        repository.delete(enrollment);
    }

    // ---------------------------------------------------------------

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();

        List<Lesson> lessons = lessonRepository.findAllByCourse(course.getId());
        Set<UUID> done = progressRepository.findByEnrollmentId(enrollment.getId()).stream()
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toSet());

        UUID next = lessons.stream()
                .filter(l -> !done.contains(l.getId()))
                .map(Lesson::getId)
                .findFirst()
                .orElse(lessons.isEmpty() ? null : lessons.get(0).getId());

        return new EnrollmentResponse(
                enrollment.getId(),
                course.getId(),
                course.getTitle(),
                course.getSlug(),
                course.getThumbnailUrl(),
                course.getInstructor().getDisplayName(),
                enrollment.getStatus(),
                done.size(),
                lessons.size(),
                percent(done.size(), lessons.size()),
                next,
                enrollment.getEnrolledAt(),
                enrollment.getLastAccessedAt());
    }

    private int percent(int done, int total) {
        return total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
    }

    private Course loadCourse(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No course with id " + id));
    }
}