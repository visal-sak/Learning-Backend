package co.istad.visal.e_learning.features.note;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LessonNoteRepository extends JpaRepository<LessonNote, UUID> {

    /** A lesson's notes in video order; untimed notes after the timed ones, newest first. */
    @Query("""
            SELECT n FROM LessonNote n
            WHERE n.user.id = :userId AND n.lesson.id = :lessonId
            ORDER BY CASE WHEN n.timestampSeconds IS NULL THEN 1 ELSE 0 END, n.timestampSeconds ASC, n.createdAt DESC
            """)
    List<LessonNote> findForLesson(@Param("userId") UUID userId, @Param("lessonId") UUID lessonId);

    /** All my notes, with lesson/section/course for listing them outside the player. */
    @Query("""
            SELECT n FROM LessonNote n
            JOIN FETCH n.lesson l JOIN FETCH l.section s JOIN FETCH s.course
            WHERE n.user.id = :userId
            ORDER BY n.updatedAt DESC
            """)
    List<LessonNote> findAllMine(@Param("userId") UUID userId);
}
