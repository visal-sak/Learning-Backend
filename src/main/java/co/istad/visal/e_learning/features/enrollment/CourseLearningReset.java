package co.istad.visal.e_learning.features.enrollment;

import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.lessoncheck.LessonCheckResultRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Leaving a course (unenroll, or removed by an admin) clears lesson-quiz scores for that course.
 * Bunny AI usage (hints, tutor questions) and extra AI help are kept on purpose, so
 * unenrolling and enrolling again can't be used to refill the AI limits.
 * Earned certificates, notes and bookmarks are kept too.
 */
@Component
public class CourseLearningReset {

    private final LessonRepository lessons;
    private final LessonCheckResultRepository checkResults;

    public CourseLearningReset(LessonRepository lessons, LessonCheckResultRepository checkResults) {
        this.lessons = lessons;
        this.checkResults = checkResults;
    }

    @Transactional
    public void reset(UUID userId, UUID courseId) {
        List<UUID> lessonIds = lessons.findAllByCourse(courseId).stream().map(Lesson::getId).toList();
        if (!lessonIds.isEmpty()) checkResults.deleteForLessons(userId, lessonIds);
    }
}
