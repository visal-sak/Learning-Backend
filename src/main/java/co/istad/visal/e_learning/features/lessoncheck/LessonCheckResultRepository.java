package co.istad.visal.e_learning.features.lessoncheck;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

import java.util.Optional;
import java.util.UUID;

public interface LessonCheckResultRepository extends JpaRepository<LessonCheckResult, UUID> {

    Optional<LessonCheckResult> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    @Modifying
    @Query("DELETE FROM LessonCheckResult r WHERE r.user.id = :userId AND r.lesson.id IN :lessonIds")
    int deleteForLessons(@Param("userId") UUID userId, @Param("lessonIds") Collection<UUID> lessonIds);
}
