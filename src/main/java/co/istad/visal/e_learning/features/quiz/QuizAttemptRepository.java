package co.istad.visal.e_learning.features.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findByEnrollmentIdAndQuizIdOrderByAttemptNoDesc(UUID enrollmentId, UUID quizId);

    long countByEnrollmentIdAndQuizId(UUID enrollmentId, UUID quizId);

    boolean existsByEnrollmentIdAndQuizIdAndPassedTrue(UUID enrollmentId, UUID quizId);

    /** The instructor's view of everyone's attempts on their own course. */
    List<QuizAttempt> findByQuizIdOrderBySubmittedAtDesc(UUID quizId);

    @Query("""
            SELECT a FROM QuizAttempt a
            WHERE a.quiz.id = :quizId AND a.enrollment.id = :enrollmentId
            ORDER BY a.score DESC
            LIMIT 1
            """)
    Optional<QuizAttempt> findBestAttempt(@Param("quizId") UUID quizId,
                                          @Param("enrollmentId") UUID enrollmentId);
}
