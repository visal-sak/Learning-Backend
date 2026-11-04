package co.istad.visal.e_learning.features.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    /** [enrollment id, lesson id] for every finished lesson, so a list of enrollments costs one query. */
    @Query("SELECT p.enrollment.id, p.lesson.id FROM LessonProgress p WHERE p.enrollment.id IN :enrollmentIds")
    List<Object[]> finishedLessons(@Param("enrollmentIds") Collection<UUID> enrollmentIds);

    boolean existsByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);

    List<LessonProgress> findByEnrollmentId(UUID enrollmentId);

    long countByEnrollmentId(UUID enrollmentId);

    void deleteByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);

    void deleteByEnrollmentId(UUID enrollmentId);
}
