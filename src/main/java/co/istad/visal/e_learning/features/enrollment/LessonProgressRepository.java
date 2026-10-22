package co.istad.visal.e_learning.features.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    boolean existsByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);

    List<LessonProgress> findByEnrollmentId(UUID enrollmentId);

    long countByEnrollmentId(UUID enrollmentId);

    void deleteByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);

    void deleteByEnrollmentId(UUID enrollmentId);
}
