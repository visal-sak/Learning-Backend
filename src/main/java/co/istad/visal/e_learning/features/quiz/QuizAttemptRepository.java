package co.istad.visal.e_learning.features.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    /** Multiple-choice ticks (join table) — a JPQL bulk delete of the answers would not clear them. */
    @Modifying
    @Query(nativeQuery = true, value = """
            DELETE FROM attempt_answer_options WHERE attempt_answer_id IN (
                SELECT a.id FROM attempt_answers a JOIN quiz_attempts q ON q.id = a.attempt_id
                WHERE q.enrollment_id = :enrollmentId)""")
    int deleteAnswerOptionsByEnrollment(@Param("enrollmentId") UUID enrollmentId);

    @Modifying
    @Query("DELETE FROM AttemptAnswer a WHERE a.attempt.id IN (SELECT q.id FROM QuizAttempt q WHERE q.enrollment.id = :enrollmentId)")
    int deleteAnswersByEnrollment(@Param("enrollmentId") UUID enrollmentId);

    @Modifying
    @Query("DELETE FROM QuizAttempt q WHERE q.enrollment.id = :enrollmentId")
    int deleteByEnrollment(@Param("enrollmentId") UUID enrollmentId);

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
