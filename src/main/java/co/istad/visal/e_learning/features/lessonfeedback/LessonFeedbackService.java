package co.istad.visal.e_learning.features.lessonfeedback;

import co.istad.visal.e_learning.expection.ForbiddenException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.lessonfeedback.dto.LessonFeedbackDtos.FeedbackResponse;
import co.istad.visal.e_learning.features.lessonfeedback.dto.LessonFeedbackDtos.MyFeedback;
import co.istad.visal.e_learning.features.lessonfeedback.dto.LessonFeedbackDtos.SaveFeedbackRequest;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Enrolled students give it; admins see all of it, instructors only their own courses'. */
@Service
@Transactional(readOnly = true)
public class LessonFeedbackService {

    private static final int LIST_LIMIT = 100;

    private final LessonFeedbackRepository feedback;
    private final LessonRepository lessons;
    private final CourseRepository courses;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;

    public LessonFeedbackService(LessonFeedbackRepository feedback, LessonRepository lessons, CourseRepository courses,
                                 CourseAccessService access, CurrentUserService currentUser) {
        this.feedback = feedback;
        this.lessons = lessons;
        this.courses = courses;
        this.access = access;
        this.currentUser = currentUser;
    }

    public MyFeedback mine(UUID lessonId) {
        User user = currentUser.require();
        return feedback.findByUserIdAndLessonId(user.getId(), lessonId)
                .map(f -> new MyFeedback(f.isHelpful(), f.getComment()))
                .orElse(new MyFeedback(null, null));
    }

    @Transactional
    public MyFeedback save(UUID lessonId, SaveFeedbackRequest request) {
        Lesson lesson = lessons.findWithCourse(lessonId)
                .orElseThrow(() -> new NotFoundException("No lesson with id " + lessonId));
        access.requireEnrollment(lesson.getSection().getCourse());

        User user = currentUser.require();
        String comment = request.comment() == null || request.comment().isBlank() ? null : request.comment().trim();
        LessonFeedback row = feedback.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseGet(() -> new LessonFeedback(user, lesson));
        row.update(request.helpful(), comment);
        feedback.save(row);
        return new MyFeedback(row.isHelpful(), row.getComment());
    }

    /** courseId null lists every course, which is for admins only. */
    public List<FeedbackResponse> list(UUID courseId) {
        boolean admin = currentUser.hasRole(KeycloakRoleEnum.ADMIN);
        if (!admin) {
            if (courseId == null) throw new ForbiddenException("Choose a course");
            Course course = courses.findById(courseId)
                    .orElseThrow(() -> new NotFoundException("No course with id " + courseId));
            if (!access.isOwner(course, currentUser.require())) {
                throw new ForbiddenException("You cannot read feedback for course " + courseId);
            }
        }
        return feedback.latest(courseId, PageRequest.of(0, LIST_LIMIT)).stream().map(LessonFeedbackService::toResponse).toList();
    }

    @Transactional
    public void delete(UUID id) {
        if (!currentUser.hasRole(KeycloakRoleEnum.ADMIN)) throw new ForbiddenException("Only admins can remove feedback");
        feedback.deleteById(id);
    }

    private static FeedbackResponse toResponse(LessonFeedback f) {
        Lesson lesson = f.getLesson();
        Course course = lesson.getSection().getCourse();
        return new FeedbackResponse(f.getId(), lesson.getId(), lesson.getTitle(), course.getId(), course.getTitle(),
                f.getUser().getDisplayName(), f.isHelpful(), f.getComment(), f.getUpdatedAt());
    }
}
