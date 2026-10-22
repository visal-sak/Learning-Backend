package co.istad.visal.e_learning.features.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, UUID> {

    boolean existsByReviewIdAndReporterId(UUID reviewId, UUID reporterId);

    List<ReviewReport> findByReviewIdAndStatus(UUID reviewId, ReportStatus status);

    long countByStatus(ReportStatus status);

    @Query("""
            SELECT r FROM ReviewReport r
            JOIN FETCH r.review rv JOIN FETCH rv.enrollment e JOIN FETCH e.user JOIN FETCH e.course
            JOIN FETCH r.reporter
            WHERE r.status = :status
            ORDER BY r.createdAt DESC
            """)
    List<ReviewReport> findWithReviewByStatus(@Param("status") ReportStatus status);
}
