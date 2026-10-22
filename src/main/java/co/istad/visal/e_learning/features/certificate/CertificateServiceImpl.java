package co.istad.visal.e_learning.features.certificate;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.certificate.dto.CertificateResponse;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.enrollment.Enrollment;
import co.istad.visal.e_learning.features.enrollment.LessonProgressRepository;
import co.istad.visal.e_learning.features.quiz.Quiz;
import co.istad.visal.e_learning.features.quiz.QuizAttempt;
import co.istad.visal.e_learning.features.quiz.QuizAttemptRepository;
import co.istad.visal.e_learning.features.quiz.QuizRepository;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Eligibility: every lesson completed, and — when the course has a quiz with
 * questions — at least one passed attempt. Both conditions are read from the
 * database here; nothing the client sends decides it.
 */
@Service
@Transactional(readOnly = true)
public class CertificateServiceImpl implements CertificateService {

    // No 0/O/1/I so a serial read aloud or typed by hand can't be misread
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CertificateRepository repository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;

    public CertificateServiceImpl(CertificateRepository repository,
                                  CourseRepository courseRepository,
                                  LessonRepository lessonRepository,
                                  LessonProgressRepository progressRepository,
                                  QuizRepository quizRepository,
                                  QuizAttemptRepository attemptRepository,
                                  CourseAccessService access,
                                  CurrentUserService currentUser) {
        this.repository = repository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.access = access;
        this.currentUser = currentUser;
    }

    @Override
    public CertificateResponse verify(String serialNo) {
        return repository.findBySerialNo(serialNo.trim().toUpperCase())
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("No certificate with serial " + serialNo));
    }

    @Override
    public List<CertificateResponse> findMine() {
        User user = currentUser.require();
        return repository.findByUserIdOrderByIssuedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public CertificateResponse claim(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("No course with id " + courseId));
        Enrollment enrollment = access.requireEnrollment(course);

        Optional<Certificate> existing = repository.findByUserIdAndCourseId(enrollment.getUser().getId(), courseId);
        if (existing.isPresent()) return toResponse(existing.get());

        String missing = missingRequirement(enrollment);
        if (missing != null) throw new ConflictException(missing);

        return toResponse(issue(enrollment));
    }

    @Override
    @Transactional
    public Optional<CertificateResponse> issueIfEligible(Enrollment enrollment) {
        UUID userId = enrollment.getUser().getId();
        UUID courseId = enrollment.getCourse().getId();
        if (repository.findByUserIdAndCourseId(userId, courseId).isPresent()) return Optional.empty();
        if (missingRequirement(enrollment) != null) return Optional.empty();
        try {
            return Optional.of(toResponse(issue(enrollment)));
        } catch (DataIntegrityViolationException raced) {
            return Optional.empty(); // issued by a parallel request
        }
    }

    // ---------------------------------------------------------------

    /** Null when eligible, otherwise what is still missing (shown to the student). */
    private String missingRequirement(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        if (!course.isCertificateEnabled()) return "This course doesn't offer a certificate";
        long total = lessonRepository.countByCourse(course.getId());
        long done = progressRepository.countByEnrollmentId(enrollment.getId());
        if (total == 0) return "This course has no lessons yet";
        if (done < total) return "Complete every lesson and quiz to earn the certificate (" + done + " of " + total + " done)";

        Optional<Quiz> quiz = quizRepository.findByCourseId(course.getId());
        if (course.isRequireFinalQuiz() && quiz.isPresent() && !quiz.get().getQuestions().isEmpty()
                && !attemptRepository.existsByEnrollmentIdAndQuizIdAndPassedTrue(enrollment.getId(), quiz.get().getId())) {
            return "Pass the final quiz to earn the certificate";
        }
        return null;
    }

    private Certificate issue(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        User student = enrollment.getUser();

        Short best = quizRepository.findByCourseId(course.getId())
                .flatMap(q -> attemptRepository.findByEnrollmentIdAndQuizIdOrderByAttemptNoDesc(enrollment.getId(), q.getId())
                        .stream().map(QuizAttempt::getScore).max(Short::compare))
                .orElse(null);

        String name = student.getDisplayName() != null && !student.getDisplayName().isBlank()
                ? student.getDisplayName()
                : student.getEmail();

        Certificate certificate = new Certificate(
                newSerial(), student, course, name, course.getTitle(),
                course.getInstructor() == null ? null : course.getInstructor().getDisplayName(), best);
        return repository.saveAndFlush(certificate);
    }

    private String newSerial() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("LN-").append(Year.now().getValue()).append('-');
            for (int i = 0; i < 8; i++) sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
            String serial = sb.toString();
            if (!repository.existsBySerialNo(serial)) return serial;
        }
        throw new IllegalStateException("Could not generate a unique certificate serial");
    }

    private CertificateResponse toResponse(Certificate c) {
        return new CertificateResponse(
                c.getSerialNo(),
                c.getStudentName(),
                c.getCourse() == null ? null : c.getCourse().getId(),
                c.getCourseTitle(),
                c.getInstructorName(),
                c.getQuizScore(),
                c.getIssuedAt());
    }
}
