package co.istad.visal.e_learning.features.note;

import co.istad.visal.e_learning.expection.ForbiddenException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.note.dto.NoteResponse;
import co.istad.visal.e_learning.features.note.dto.SaveNoteRequest;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Notes are private: every read and write is scoped to the signed-in user, and
 * a note can only be added to a lesson that user is allowed to watch.
 */
@Service
@Transactional(readOnly = true)
public class LessonNoteService {

    private final LessonNoteRepository repository;
    private final LessonRepository lessonRepository;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;

    public LessonNoteService(LessonNoteRepository repository, LessonRepository lessonRepository,
                             CourseAccessService access, CurrentUserService currentUser) {
        this.repository = repository;
        this.lessonRepository = lessonRepository;
        this.access = access;
        this.currentUser = currentUser;
    }

    public List<NoteResponse> forLesson(UUID lessonId) {
        User user = currentUser.require();
        return repository.findForLesson(user.getId(), lessonId).stream().map(this::toResponse).toList();
    }

    public List<NoteResponse> mine() {
        User user = currentUser.require();
        return repository.findAllMine(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(SaveNoteRequest request) {
        User user = currentUser.require();
        if (request.lessonId() == null) throw new NotFoundException("Choose the lesson for this note");
        Lesson lesson = lessonRepository.findWithCourse(request.lessonId())
                .orElseThrow(() -> new NotFoundException("No lesson with id " + request.lessonId()));
        access.requireCanViewContent(lesson);
        return toResponse(repository.save(new LessonNote(user, lesson, request.timestampSeconds(), request.body().trim())));
    }

    @Transactional
    public NoteResponse update(UUID noteId, SaveNoteRequest request) {
        LessonNote note = own(noteId);
        note.setBody(request.body().trim());
        note.setTimestampSeconds(request.timestampSeconds());
        return toResponse(note);
    }

    @Transactional
    public void delete(UUID noteId) {
        repository.delete(own(noteId));
    }

    private LessonNote own(UUID noteId) {
        User user = currentUser.require();
        LessonNote note = repository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("No note with id " + noteId));
        if (!note.getUser().getId().equals(user.getId())) throw new ForbiddenException("That note is not yours");
        return note;
    }

    private NoteResponse toResponse(LessonNote n) {
        Lesson lesson = n.getLesson();
        Course course = lesson.getSection().getCourse();
        return new NoteResponse(
                n.getId(), lesson.getId(), lesson.getTitle(), course.getId(), course.getTitle(),
                n.getTimestampSeconds(), n.getBody(), n.getCreatedAt(), n.getUpdatedAt());
    }
}
