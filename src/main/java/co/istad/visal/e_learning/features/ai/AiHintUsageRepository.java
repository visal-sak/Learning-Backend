package co.istad.visal.e_learning.features.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AiHintUsageRepository extends JpaRepository<AiHintUsage, UUID> {

    String LESSON_CHECK = "LESSON_CHECK";
    String FINAL_QUIZ = "FINAL_QUIZ";
    /** Lesson AI tutor questions (refId = lesson id, attemptNo = 0: the limit is per lesson, not per attempt). */
    String TUTOR = "TUTOR";

    Optional<AiHintUsage> findByUserIdAndScopeAndRefIdAndAttemptNo(UUID userId, String scope, UUID refId, int attemptNo);

    /** Hints used on this quiz over all attempts — the limit is per lesson / quiz, so retaking doesn't refill it. */
    @Query("SELECT COALESCE(SUM(h.used), 0) FROM AiHintUsage h WHERE h.userId = :userId AND h.scope = :scope AND h.refId = :refId")
    int totalUsed(@Param("userId") UUID userId, @Param("scope") String scope, @Param("refId") UUID refId);

    /** Hints used on one attempt (for the optional score deduction). */
    default int used(UUID userId, String scope, UUID refId, int attemptNo) {
        return findByUserIdAndScopeAndRefIdAndAttemptNo(userId, scope, refId, attemptNo).map(AiHintUsage::getUsed).orElse(0);
    }
}
