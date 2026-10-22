package co.istad.visal.e_learning.features.report;

import co.istad.visal.e_learning.features.course.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Reads only, and only aggregates. Bound to Course because Spring Data needs
 * an entity, but nothing here writes.
 */
public interface ReportRepository extends JpaRepository<Course, UUID> {

    @Query("""
            SELECT c.id, c.title, c.thumbnailUrl, c.instructor.displayName, c.category.name,
                   COUNT(DISTINCT e.id),
                   COALESCE(AVG(r.rating), 0),
                   COUNT(DISTINCT r.id)
            FROM Course c
            LEFT JOIN Enrollment e ON e.course = c
            LEFT JOIN Review r ON r.enrollment.course = c
            WHERE c.status = co.istad.visal.e_learning.features.course.CourseStatus.PUBLISHED
            GROUP BY c.id, c.title, c.thumbnailUrl, c.instructor.displayName, c.category.name
            ORDER BY COUNT(DISTINCT e.id) DESC
            """)
    List<Object[]> topByEnrollment(Pageable pageable);

    /**
     * HAVING COUNT >= 3 matters. Without it a single five-star review beats a
     * course with a hundred reviews averaging 4.8.
     */
    @Query("""
            SELECT c.id, c.title, c.thumbnailUrl, c.instructor.displayName, c.category.name,
                   COUNT(DISTINCT e.id),
                   AVG(r.rating),
                   COUNT(DISTINCT r.id)
            FROM Course c
            LEFT JOIN Enrollment e ON e.course = c
            JOIN Review r ON r.enrollment.course = c
            WHERE c.status = co.istad.visal.e_learning.features.course.CourseStatus.PUBLISHED
            GROUP BY c.id, c.title, c.thumbnailUrl, c.instructor.displayName, c.category.name
            HAVING COUNT(DISTINCT r.id) >= 3
            ORDER BY AVG(r.rating) DESC
            """)
    List<Object[]> topByRating(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT c.instructor.id) FROM Course c")
    long countInstructors();

    @Query("SELECT COUNT(l) FROM Lesson l")
    long countLessons();

    @Query("SELECT COUNT(a) FROM QuizAttempt a")
    long countQuizAttempts();

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r")
    double platformAverageRating();

    // ----- instructor scoped -----

    @Query("SELECT COUNT(c) FROM Course c WHERE c.instructor.id = :id")
    long countCoursesByInstructor(@Param("id") UUID id);

    @Query("""
            SELECT COUNT(c) FROM Course c
            WHERE c.instructor.id = :id
              AND c.status = co.istad.visal.e_learning.features.course.CourseStatus.PUBLISHED
            """)
    long countPublishedByInstructor(@Param("id") UUID id);

    /** DISTINCT: one person enrolled in three of your courses is one student. */
    @Query("SELECT COUNT(DISTINCT e.user.id) FROM Enrollment e WHERE e.course.instructor.id = :id")
    long countStudentsByInstructor(@Param("id") UUID id);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.section.course.instructor.id = :id")
    long countLessonsByInstructor(@Param("id") UUID id);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.enrollment.course.instructor.id = :id")
    double averageRatingByInstructor(@Param("id") UUID id);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.enrollment.course.instructor.id = :id")
    long countReviewsByInstructor(@Param("id") UUID id);
}
