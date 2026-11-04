package co.istad.visal.e_learning.features.lessonfeedback;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonFeedbackRepository extends JpaRepository<LessonFeedback, UUID> {

    Optional<LessonFeedback> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    /** Newest first. courseId null means every course. */
    @Query("""
            SELECT f FROM LessonFeedback f
            JOIN FETCH f.user
            JOIN FETCH f.lesson l
            JOIN FETCH l.section s
            JOIN FETCH s.course c
            WHERE (:courseId IS NULL OR c.id = :courseId)
            ORDER BY f.updatedAt DESC
            """)
    List<LessonFeedback> latest(@Param("courseId") UUID courseId, Pageable pageable);
}
