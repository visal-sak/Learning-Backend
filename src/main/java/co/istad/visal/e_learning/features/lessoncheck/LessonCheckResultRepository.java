package co.istad.visal.e_learning.features.lessoncheck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LessonCheckResultRepository extends JpaRepository<LessonCheckResult, UUID> {

    Optional<LessonCheckResult> findByUserIdAndLessonId(UUID userId, UUID lessonId);
}
