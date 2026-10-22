package co.istad.visal.e_learning.features.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    Optional<Quiz> findByCourseId(UUID courseId);

    boolean existsByCourseId(UUID courseId);
}
