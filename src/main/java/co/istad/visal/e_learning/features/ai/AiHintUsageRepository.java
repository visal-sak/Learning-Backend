package co.istad.visal.e_learning.features.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiHintUsageRepository extends JpaRepository<AiHintUsage, UUID> {

    String LESSON_CHECK = "LESSON_CHECK";
    String FINAL_QUIZ = "FINAL_QUIZ";
    /** Lesson AI tutor questions (refId = lesson id, attemptNo = 0: the limit is per lesson, not per attempt). */
    String TUTOR = "TUTOR";

    Optional<AiHintUsage> findByUserIdAndScopeAndRefIdAndAttemptNo(UUID userId, String scope, UUID refId, int attemptNo);

    default int used(UUID userId, String scope, UUID refId, int attemptNo) {
        return findByUserIdAndScopeAndRefIdAndAttemptNo(userId, scope, refId, attemptNo).map(AiHintUsage::getUsed).orElse(0);
    }
}
