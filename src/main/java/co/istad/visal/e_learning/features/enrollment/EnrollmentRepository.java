package co.istad.visal.e_learning.features.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    /** The query behind every student-side access check. */
    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

    List<Enrollment> findByUserIdOrderByLastAccessedAtDesc(UUID userId);

    List<Enrollment> findByCourseIdOrderByEnrolledAtDesc(UUID courseId);


    long countByCourseId(UUID courseId);

    @Query("SELECT COUNT(p) FROM LessonProgress p WHERE p.enrollment.id = :enrollmentId")
    long countCompletedLessons(@Param("enrollmentId") UUID enrollmentId);
}
