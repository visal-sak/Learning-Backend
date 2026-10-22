package co.istad.visal.e_learning.features.bookmark;

import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.bookmark.dto.BookmarkResponse;
import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CurrentUserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository repository;
    private final LessonRepository lessonRepository;
    private final CurrentUserService currentUser;

    public BookmarkService(BookmarkRepository repository, LessonRepository lessonRepository, CurrentUserService currentUser) {
        this.repository = repository;
        this.lessonRepository = lessonRepository;
        this.currentUser = currentUser;
    }

    public List<BookmarkResponse> findMine() {
        User user = currentUser.require();
        return repository.findMine(user.getId()).stream().map(this::toResponse).toList();
    }

    /** Idempotent: bookmarking twice keeps one bookmark. */
    @Transactional
    public BookmarkResponse add(UUID lessonId) {
        User user = currentUser.require();
        Lesson lesson = lessonRepository.findWithCourse(lessonId)
                .orElseThrow(() -> new NotFoundException("No lesson with id " + lessonId));

        return repository.findByUserIdAndLessonId(user.getId(), lessonId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    try {
                        return toResponse(repository.saveAndFlush(new Bookmark(user, lesson)));
                    } catch (DataIntegrityViolationException doubleClick) {
                        return toResponse(repository.findByUserIdAndLessonId(user.getId(), lessonId).orElseThrow());
                    }
                });
    }

    /** Idempotent: removing a bookmark that isn't there is not an error. */
    @Transactional
    public void remove(UUID lessonId) {
        User user = currentUser.require();
        repository.findByUserIdAndLessonId(user.getId(), lessonId).ifPresent(repository::delete);
    }

    private BookmarkResponse toResponse(Bookmark b) {
        Lesson lesson = b.getLesson();
        Course course = lesson.getSection().getCourse();
        return new BookmarkResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDurationMinutes(),
                lesson.getSection().getId(),
                lesson.getSection().getTitle(),
                course.getId(),
                course.getTitle(),
                course.getThumbnailUrl(),
                b.getCreatedAt());
    }
}
