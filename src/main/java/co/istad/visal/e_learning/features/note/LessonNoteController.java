package co.istad.visal.e_learning.features.note;

import co.istad.visal.e_learning.features.note.dto.NoteResponse;
import co.istad.visal.e_learning.features.note.dto.SaveNoteRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Under /my/** — notes always belong to the signed-in user. */
@RestController
@RequestMapping("/api/v1/my/notes")
public class LessonNoteController {

    private final LessonNoteService service;

    public LessonNoteController(LessonNoteService service) {
        this.service = service;
    }

    /** ?lessonId= for one lesson's notes (video order); without it, all my notes. */
    @GetMapping
    public List<NoteResponse> list(@RequestParam(required = false) UUID lessonId) {
        return lessonId == null ? service.mine() : service.forLesson(lessonId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@Valid @RequestBody SaveNoteRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable UUID id, @Valid @RequestBody SaveNoteRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
