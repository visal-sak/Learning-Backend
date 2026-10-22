package co.istad.visal.e_learning.features.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AiAccessGrantRepository extends JpaRepository<AiAccessGrant, UUID> {

    List<AiAccessGrant> findByCourseIdOrderByCreatedAtDesc(UUID courseId);

    @Query("SELECT COALESCE(SUM(g.extraQuestions), 0) FROM AiAccessGrant g WHERE g.userId = :userId AND g.lessonId = :lessonId")
    int extraQuestions(@Param("userId") UUID userId, @Param("lessonId") UUID lessonId);

    @Query("SELECT COALESCE(SUM(g.extraHints), 0) FROM AiAccessGrant g WHERE g.userId = :userId AND g.lessonId = :lessonId")
    int extraHints(@Param("userId") UUID userId, @Param("lessonId") UUID lessonId);
}
