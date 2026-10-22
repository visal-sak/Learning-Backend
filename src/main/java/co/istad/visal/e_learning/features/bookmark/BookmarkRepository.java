package co.istad.visal.e_learning.features.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    Optional<Bookmark> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    @Query("""
            SELECT b FROM Bookmark b
            JOIN FETCH b.lesson l JOIN FETCH l.section s JOIN FETCH s.course
            WHERE b.user.id = :userId
            ORDER BY b.createdAt DESC
            """)
    List<Bookmark> findMine(@Param("userId") UUID userId);
}
