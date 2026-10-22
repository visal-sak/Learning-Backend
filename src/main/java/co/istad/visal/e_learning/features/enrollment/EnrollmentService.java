package co.istad.visal.e_learning.features.enrollment;

import co.istad.visal.e_learning.features.enrollment.dto.EnrollmentResponse;
import co.istad.visal.e_learning.features.enrollment.dto.ProgressResponse;
import co.istad.visal.e_learning.features.enrollment.dto.StudentProgressResponse;

import java.util.List;
import java.util.UUID;

public interface EnrollmentService {

    /** One click, empty body. User from the token, course from the path. */
    EnrollmentResponse enroll(UUID courseId);

    /** My learning. */
    List<EnrollmentResponse> findMine();

    EnrollmentResponse findMineByCourse(UUID courseId);

    ProgressResponse markLessonComplete(UUID lessonId);

    ProgressResponse unmarkLessonComplete(UUID lessonId);

    /** Instructor view of their own course. */
    List<StudentProgressResponse> findStudents(UUID courseId);

    /** The student leaving, or an admin removing them. */
    void unenroll(UUID courseId);

    void removeStudent(UUID courseId, UUID userId);
}
