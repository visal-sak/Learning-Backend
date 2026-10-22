package co.istad.visal.e_learning.features.review;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.ForbiddenException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.enrollment.Enrollment;
import co.istad.visal.e_learning.features.enrollment.LessonProgressRepository;
import co.istad.visal.e_learning.features.review.dto.RatingSummaryResponse;
import co.istad.visal.e_learning.features.review.dto.ReviewResponse;
import co.istad.visal.e_learning.features.review.dto.SaveReviewRequest;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import co.istad.visal.e_learning.utils.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    /**
     * A review from someone who watched nothing is noise. Requiring one
     * completed lesson is low enough not to annoy anyone and high enough to
     * stop drive-by ratings.
     */
    private static final int MIN_LESSONS_BEFORE_REVIEW = 1;

    private final ReviewRepository repository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;

    public ReviewServiceImpl(ReviewRepository repository,
                             CourseRepository courseRepository,
                             LessonRepository lessonRepository,
                             LessonProgressRepository progressRepository,
                             CourseAccessService access,
                             CurrentUserService currentUser) {
        this.repository = repository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.access = access;
        this.currentUser = currentUser;
    }

    // ---------------------------------------------------------------
    //  Reads — public
    // ---------------------------------------------------------------

    @Override
    public PageResponse<ReviewResponse> findByCourse(UUID courseId, Pageable pageable) {
        Course course = loadCourse(courseId);
        access.requireCanView(course);

        return PageResponse.from(repository.findByCourse(courseId, pageable), this::toResponse);
    }

    @Override
    public RatingSummaryResponse summary(UUID courseId) {
        Course course = loadCourse(courseId);
        access.requireCanView(course);

        Double avg = repository.averageRating(courseId);
        long total = repository.countByCourse(courseId);

        Map<Short, Long> distribution = new HashMap<>();
        for (short i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        for (Object[] row : repository.ratingDistribution(courseId)) {
            distribution.put((Short) row[0], (Long) row[1]);
        }

        // Rounded to one decimal. Raw AVG gives 4.333333333333333, which is
        // both wrong-looking and more precision than the data supports.
        double rounded = avg == null ? 0 : Math.round(avg * 10) / 10.0;

        return new RatingSummaryResponse(rounded, total, distribution);
    }

    @Override
    public ReviewResponse findMine(UUID courseId) {
        Course course = loadCourse(courseId);

        Enrollment enrollment = access.activeEnrollment(course)
                .orElseThrow(() -> new NotFoundException("You are not enrolled in this course"));

        return repository.findByEnrollmentId(enrollment.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("You have not reviewed this course"));
    }

    // ---------------------------------------------------------------
    //  Write
    // ---------------------------------------------------------------

    /**
     * Create and update are one method. A review is one per enrollment, so
     * "save" is the honest verb — a second POST edits rather than duplicates,
     * and the client does not have to know which it is doing.
     */
    @Override
    @Transactional
    public ReviewResponse save(UUID courseId, SaveReviewRequest request) {
        Course course = loadCourse(courseId);
        Enrollment enrollment = requireEnrollment(courseId);

        long done = progressRepository.countByEnrollmentId(enrollment.getId());
        if (done < MIN_LESSONS_BEFORE_REVIEW) {
            throw new ConflictException("Complete at least one lesson before reviewing this course");
        }

        Review review = repository.findByEnrollmentId(enrollment.getId())
                .orElseGet(() -> new Review(enrollment, request.rating(), request.comment()));

        review.setRating(request.rating());
        review.setComment(request.comment());

        return toResponse(repository.save(review));
    }

    @Override
    @Transactional
    public void deleteMine(UUID courseId) {
        Enrollment enrollment = requireEnrollment(courseId);

        Review review = repository.findByEnrollmentId(enrollment.getId())
                .orElseThrow(() -> new NotFoundException("You have not reviewed this course"));

        repository.delete(review);
    }

    @Override
    @Transactional
    public void deleteAny(UUID reviewId) {
        if (!currentUser.hasRole(KeycloakRoleEnum.ADMIN)) {
            throw new ForbiddenException("Only an admin can remove someone else's review");
        }

        Review review = repository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("No review with id " + reviewId));

        repository.delete(review);
    }

    // ---------------------------------------------------------------
    //  Instructor
    // ---------------------------------------------------------------

    /**
     * Note what is absent: no reply, no delete. An instructor who can remove
     * a one-star review can manufacture a five-star average, which makes
     * every rating on the platform worthless.
     */
    @Override
    public PageResponse<ReviewResponse> findForInstructor(Pageable pageable) {
        User user = currentUser.require();
        return PageResponse.from(repository.findByInstructor(user.getId(), pageable), this::toResponse);
    }

    // ---------------------------------------------------------------

    private ReviewResponse toResponse(Review review) {
        Enrollment enrollment = review.getEnrollment();
        User author = enrollment.getUser();
        Course course = enrollment.getCourse();

        int total = (int) lessonRepository.countByCourse(course.getId());
        int done = (int) progressRepository.countByEnrollmentId(enrollment.getId());
        int percent = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);

        boolean mine = currentUser.current()
                .map(u -> u.getId().equals(author.getId()))
                .orElse(false);

        return new ReviewResponse(
                review.getId(),
                course.getId(),
                author.getId(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                review.getRating(),
                review.getComment(),
                percent,
                mine,
                review.getCreatedAt(),
                review.getUpdatedAt());
    }

    private Enrollment requireEnrollment(UUID courseId) {
        Course course = loadCourse(courseId);
        return access.requireEnrollment(course);
    }

    private Course loadCourse(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No course with id " + id));
    }
}
