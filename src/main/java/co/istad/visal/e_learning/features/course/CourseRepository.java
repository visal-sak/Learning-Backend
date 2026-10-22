package co.istad.visal.e_learning.features.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Course> findByInstructorIdOrderByUpdatedAtDesc(UUID instructorId);

    List<Course> findByStatusOrderBySubmittedAtAsc(CourseStatus status);

    /**
     * The public catalog. Status is pinned to PUBLISHED in the query rather
     * than passed in, so no caller can widen it by accident.
     *
     * The search term arrives already lowercased and wrapped in % from the
     * service. Building the pattern here with CONCAT around a null parameter
     * is what produced "function lower(bytea) does not exist" — Postgres
     * cannot infer a type for an untyped null.
     */
    @Query("""
            SELECT c FROM Course c
            WHERE c.status = co.istad.visal.e_learning.features.course.CourseStatus.PUBLISHED
              AND (:categoryId IS NULL OR c.category.id = :categoryId)
              AND (:level IS NULL OR c.level = :level)
              AND (LOWER(c.title) LIKE :q OR LOWER(c.summary) LIKE :q)
            """)
    Page<Course> search(@Param("q") String q,
                        @Param("categoryId") UUID categoryId,
                        @Param("level") CourseLevel level,
                        Pageable pageable);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId")
    long countEnrollments(@Param("courseId") UUID courseId);
}