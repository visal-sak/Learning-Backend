package co.istad.visal.e_learning.features.enrollment;

import co.istad.visal.e_learning.features.enrollment.dto.EnrollmentResponse;
import co.istad.visal.e_learning.features.enrollment.dto.ProgressResponse;
import co.istad.visal.e_learning.features.enrollment.dto.StudentProgressResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class EnrollmentController {

    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    /**
     * The whole enroll feature. No request body at all: the user comes from
     * the token and the course from the path, so there is nothing a client
     * could tamper with.
     */
    @PostMapping("/courses/{courseId}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@PathVariable UUID courseId) {
        return service.enroll(courseId);
    }

    @DeleteMapping("/courses/{courseId}/enroll")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable UUID courseId) {
        service.unenroll(courseId);
    }

    @GetMapping("/my/enrollments")
    public List<EnrollmentResponse> myLearning() {
        return service.findMine();
    }

    @GetMapping("/my/enrollments/{courseId}")
    public EnrollmentResponse myEnrollment(@PathVariable UUID courseId) {
        return service.findMineByCourse(courseId);
    }

    @PostMapping("/lessons/{lessonId}/complete")
    public ProgressResponse markComplete(@PathVariable UUID lessonId) {
        return service.markLessonComplete(lessonId);
    }

    @DeleteMapping("/lessons/{lessonId}/complete")
    public ProgressResponse unmarkComplete(@PathVariable UUID lessonId) {
        return service.unmarkLessonComplete(lessonId);
    }

    /** Instructors see their own course only; the service enforces that. */
    @GetMapping("/courses/{courseId}/students")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public List<StudentProgressResponse> students(@PathVariable UUID courseId) {
        return service.findStudents(courseId);
    }

    @DeleteMapping("/courses/{courseId}/students/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudent(@PathVariable UUID courseId, @PathVariable UUID userId) {
        service.removeStudent(courseId, userId);
    }
}
