package co.istad.visal.e_learning.features.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByEnrollmentId(UUID enrollmentId);

    boolean existsByEnrollmentId(UUID enrollmentId);

    @Query("""
            SELECT r FROM Review r
            WHERE r.enrollment.course.id = :courseId
            ORDER BY r.createdAt DESC
            """)
    Page<Review> findByCourse(@Param("courseId") UUID courseId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.enrollment.course.id = :courseId")
    Double averageRating(@Param("courseId") UUID courseId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.enrollment.course.id = :courseId")
    long countByCourse(@Param("courseId") UUID courseId);

    /** Rows of [rating, count] for the distribution bars. */
    @Query("""
            SELECT r.rating, COUNT(r) FROM Review r
            WHERE r.enrollment.course.id = :courseId
            GROUP BY r.rating
            """)
    List<Object[]> ratingDistribution(@Param("courseId") UUID courseId);

    /** Every review across one instructor's courses. */
    @Query("""
            SELECT r FROM Review r
            WHERE r.enrollment.course.instructor.id = :instructorId
            ORDER BY r.createdAt DESC
            """)
    Page<Review> findByInstructor(@Param("instructorId") UUID instructorId, Pageable pageable);
}
