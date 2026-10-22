package co.istad.visal.e_learning.features.enrollment;

import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.lessoncheck.LessonCheckResultRepository;
import co.istad.visal.e_learning.features.quiz.QuizAttemptRepository;
import co.istad.visal.e_learning.features.review.ReviewRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Leaving a course (unenroll, or removed by an admin): clears what belongs to the enrollment
 * so it can be deleted — final-quiz attempts and the course review reference it — plus
 * lesson-quiz scores for that course.
 * Bunny AI usage (hints, tutor questions) and extra AI help are kept on purpose, so
 * unenrolling and enrolling again can't be used to refill the AI limits.
 * Earned certificates, notes and bookmarks are kept too.
 */
@Component
public class CourseLearningReset {

    private final LessonRepository lessons;
    private final LessonCheckResultRepository checkResults;
    private final QuizAttemptRepository attempts;
    private final ReviewRepository reviews;

    public CourseLearningReset(LessonRepository lessons, LessonCheckResultRepository checkResults,
                               QuizAttemptRepository attempts, ReviewRepository reviews) {
        this.lessons = lessons;
        this.checkResults = checkResults;
        this.attempts = attempts;
        this.reviews = reviews;
    }

    /** Call before deleting the enrollment. */
    @Transactional
    public void reset(Enrollment enrollment) {
        UUID enrollmentId = enrollment.getId();
        attempts.deleteAnswerOptionsByEnrollment(enrollmentId);
        attempts.deleteAnswersByEnrollment(enrollmentId);
        attempts.deleteByEnrollment(enrollmentId);
        reviews.deleteByEnrollment(enrollmentId);

        List<UUID> lessonIds = lessons.findAllByCourse(enrollment.getCourse().getId()).stream().map(Lesson::getId).toList();
        if (!lessonIds.isEmpty()) checkResults.deleteForLessons(enrollment.getUser().getId(), lessonIds);
    }
}
