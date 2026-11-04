package co.istad.visal.e_learning.features.course;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.IllegalTransitionException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.category.Category;
import co.istad.visal.e_learning.features.category.CategoryRepository;
import co.istad.visal.e_learning.features.content.SectionMapper;
import co.istad.visal.e_learning.features.content.dto.SectionResponse;
import co.istad.visal.e_learning.features.enrollment.LessonProgressRepository;
import co.istad.visal.e_learning.features.course.dto.*;
import co.istad.visal.e_learning.features.enrollment.Enrollment;
import co.istad.visal.e_learning.features.enrollment.EnrollmentRepository;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.features.user.UserMapper;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import co.istad.visal.e_learning.utils.PageResponse;
import co.istad.visal.e_learning.utils.SlugUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository repository;
    private final CategoryRepository categoryRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseMapper mapper;
    private final SectionMapper sectionMapper;
    private final UserMapper userMapper;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;
    private final co.istad.visal.e_learning.features.quiz.QuizRepository quizRepository;

    public CourseServiceImpl(CourseRepository repository,
                             CategoryRepository categoryRepository,
                             EnrollmentRepository enrollmentRepository,
                             LessonProgressRepository progressRepository,
                             CourseMapper mapper,
                             SectionMapper sectionMapper,
                             UserMapper userMapper,
                             CourseAccessService access,
                             CurrentUserService currentUser,
                             co.istad.visal.e_learning.features.quiz.QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.progressRepository = progressRepository;
        this.mapper = mapper;
        this.sectionMapper = sectionMapper;
        this.userMapper = userMapper;
        this.access = access;
        this.currentUser = currentUser;
    }

    @Override
    public PageResponse<CourseSummaryResponse> search(String q, UUID categoryId, CourseLevel level, Pageable pageable) {
        // "%" matches every row, so an absent search term behaves as no filter
        // — and there is no untyped null for Postgres to guess a type for,
        // which is what produced "function lower(bytea) does not exist".
        String term = (q == null || q.isBlank())
                ? "%"
                : "%" + q.trim().toLowerCase() + "%";

        return PageResponse.from(repository.search(term, categoryId, level, pageable), mapper::toSummary);
    }

    @Override
    public CourseDetailResponse getById(UUID id) {
        Course course = load(id);
        access.requireCanView(course);
        return toDetail(course);
    }

    @Override
    public CourseDetailResponse getBySlug(String slug) {
        Course course = repository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("No course with slug " + slug));
        access.requireCanView(course);
        return toDetail(course);
    }

    @Override
    public List<CourseSummaryResponse> findMine() {
        User user = currentUser.require();

        List<Course> courses = currentUser.hasRole(KeycloakRoleEnum.ADMIN)
                ? repository.findAll()
                : repository.findByInstructorIdOrderByUpdatedAtDesc(user.getId());

        return courses.stream().map(mapper::toSummary).toList();
    }

    @Override
    public List<CourseSummaryResponse> findPendingReview() {
        return repository.findByStatusOrderBySubmittedAtAsc(CourseStatus.PENDING_REVIEW)
                .stream().map(mapper::toSummary).toList();
    }

    @Override
    @Transactional
    public CourseDetailResponse create(CreateCourseRequest request) {
        User instructor = currentUser.require();
        Category category = loadCategory(request.categoryId());
        String title = request.title().trim();

        Course course = new Course(instructor, category, title,
                SlugUtils.unique(title, "course", repository::existsBySlug));

        course.setSummary(request.summary());
        course.setDescription(request.description());
        // The thumbnail uploaded on the "New course" form (it was dropped before, so new courses had no image)
        course.setThumbnailUrl(request.thumbnailUrl() == null || request.thumbnailUrl().isBlank() ? null : request.thumbnailUrl());

        if (request.level() != null) {
            course.setLevel(request.level());
        }

        return toDetail(repository.save(course));
    }

    @Override
    @Transactional
    public CourseDetailResponse update(UUID id, UpdateCourseRequest request) {
        Course course = load(id);
        access.requireCanEdit(course);

        course.setTitle(request.title().trim());
        course.setSummary(request.summary());
        course.setDescription(request.description());
        course.setThumbnailUrl(request.thumbnailUrl());

        if (request.level() != null) {
            course.setLevel(request.level());
        }
        if (request.categoryId() != null) {
            course.setCategory(loadCategory(request.categoryId()));
        }
        if (request.requireQuizPass() != null) course.setRequireQuizPass(request.requireQuizPass());
        if (request.requireFinalQuiz() != null) course.setRequireFinalQuiz(request.requireFinalQuiz());
        if (request.certificateEnabled() != null) course.setCertificateEnabled(request.certificateEnabled());
        if (request.aiHelperEnabled() != null) course.setAiHelperEnabled(request.aiHelperEnabled());
        if (request.aiHintsPerAttempt() != null) course.setAiHintsPerAttempt(request.aiHintsPerAttempt());
        if (request.aiInFinalQuiz() != null) course.setAiInFinalQuiz(request.aiInFinalQuiz());
        if (request.aiExplainEnabled() != null) course.setAiExplainEnabled(request.aiExplainEnabled());
        if (request.aiHintPenalty() != null) course.setAiHintPenalty(request.aiHintPenalty());

        // The slug is not regenerated on rename: it is already in public URLs
        // and in links people have shared.

        return toDetail(course);
    }

    @Override
    @Transactional
    public CourseDetailResponse changeStatus(UUID id, ChangeStatusRequest request) {
        Course course = load(id);
        CourseStatus target = request.status();

        KeycloakRoleEnum role = currentUser.hasRole(KeycloakRoleEnum.ADMIN)
                ? KeycloakRoleEnum.ADMIN
                : KeycloakRoleEnum.INSTRUCTOR;

        // Admins move any course. Instructors move their own, and only along
        // transitions the state machine allows them.
        if (role != KeycloakRoleEnum.ADMIN) {
            User user = currentUser.require();
            if (!access.isOwner(course, user)) {
                // 404, not 403: a 403 here confirms the course exists.
                throw new NotFoundException("No course with id " + id);
            }
        }

        if (!course.getStatus().canMoveTo(target, role)) {
            throw new IllegalTransitionException(course.getStatus(), target);
        }

        if (target == CourseStatus.REJECTED && (request.note() == null || request.note().isBlank())) {
            throw new ConflictException("A rejection needs a reason the instructor can act on");
        }

        if (target == CourseStatus.PENDING_REVIEW && course.totalLessons() == 0) {
            throw new ConflictException("Add at least one lesson before submitting for review");
        }

        course.moveTo(target, request.note());
        return toDetail(course);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Course course = load(id);
        access.requireCanEdit(course);

        long enrolled = repository.countEnrollments(id);
        if (enrolled > 0) {
            throw new ConflictException(
                    "This course has " + enrolled + " enrolled student(s). Archive it instead of deleting.");
        }

        // Not cascaded from the course. Attempts on the quiz stop this with a 409 from the handler.
        quizRepository.findByCourseId(id).ifPresent(quizRepository::delete);
        repository.delete(course);
    }

    // ---------------------------------------------------------------

    private CourseDetailResponse toDetail(Course course) {
        Optional<Enrollment> enrollment = access.activeEnrollment(course);
        boolean editable = access.canEdit(course);

        Integer percent = enrollment
                .map(e -> percentComplete(course, e))
                .orElse(null);

        Set<UUID> done = enrollment
                .map(e -> progressRepository.findByEnrollmentId(e.getId()).stream()
                        .map(p -> p.getLesson().getId())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());

        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getSlug(),
                course.getSummary(),
                course.getDescription(),
                course.getThumbnailUrl(),
                course.getLevel(),
                course.getStatus(),
                course.getCategory().getName(),
                course.getCategory().getId(),
                userMapper.toPublicProfile(course.getInstructor()),
                course.getSections().stream().map(sectionMapper::toResponse).map(s -> SectionResponse.markProgress(s, done)).toList(),
                course.totalLessons(),
                course.totalMinutes(),
                enrollment.isPresent(),
                editable,
                percent,
                editable ? course.getReviewNote() : null,
                course.getPublishedAt(),
                course.isRequireQuizPass(),
                course.isRequireFinalQuiz(),
                course.isCertificateEnabled(),
                quizRepository.findByCourseId(course.getId()).map(q -> q.getQuestions().size()).orElse(0),
                course.isAiHelperEnabled(),
                course.getAiHintsPerAttempt(),
                course.isAiInFinalQuiz(),
                course.isAiExplainEnabled(),
                course.getAiHintPenalty());
    }

    private int percentComplete(Course course, Enrollment enrollment) {
        int total = course.totalLessons();
        if (total == 0) {
            return 0;
        }
        long done = enrollmentRepository.countCompletedLessons(enrollment.getId());
        return (int) Math.round(done * 100.0 / total);
    }

    private Course load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No course with id " + id));
    }

    private Category loadCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No category with id " + id));
    }
}