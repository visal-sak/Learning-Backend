package co.istad.visal.e_learning.features.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findBySectionIdOrderByPositionAsc(UUID sectionId);

    long countBySectionId(UUID sectionId);

    /** Every lesson in a course, in curriculum order. Used for "next lesson" and progress. */
    @Query("""
            SELECT l FROM Lesson l
            JOIN l.section s
            WHERE s.course.id = :courseId
            ORDER BY s.position ASC, l.position ASC
            """)
    List<Lesson> findAllByCourse(@Param("courseId") UUID courseId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.section.course.id = :courseId")
    long countByCourse(@Param("courseId") UUID courseId);

    @Query("SELECT l FROM Lesson l JOIN FETCH l.section s JOIN FETCH s.course WHERE l.id = :id")
    Optional<Lesson> findWithCourse(@Param("id") UUID id);
}
