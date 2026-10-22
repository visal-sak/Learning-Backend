package co.istad.visal.e_learning.features.content;

import co.istad.visal.e_learning.features.content.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ContentController {

    private final ContentService service;

    public ContentController(ContentService service) {
        this.service = service;
    }

    // ---------- reads ----------

    /** Public: the outline anyone can see before enrolling. */
    @GetMapping("/courses/{courseId}/curriculum")
    public List<SectionResponse> curriculum(@PathVariable UUID courseId) {
        return service.getCurriculum(courseId);
    }

    /**
     * Gated: preview lessons pass for anyone, everything else needs an
     * enrollment, ownership or the ADMIN role. No @PreAuthorize here on
     * purpose — a role check cannot express any of those.
     */
    @GetMapping("/lessons/{lessonId}")
    public LessonContentResponse lesson(@PathVariable UUID lessonId) {
        return service.getLessonContent(lessonId);
    }

    // ---------- sections ----------

    @PostMapping("/sections")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse createSection(@Valid @RequestBody CreateSectionRequest request) {
        return service.createSection(request);
    }

    @PutMapping("/sections/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public SectionResponse updateSection(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateSectionRequest request) {
        return service.updateSection(id, request);
    }

    @DeleteMapping("/sections/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable UUID id) {
        service.deleteSection(id);
    }

    @PatchMapping("/courses/{courseId}/sections/order")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderSections(@PathVariable UUID courseId, @Valid @RequestBody ReorderRequest request) {
        service.reorderSections(courseId, request);
    }

    // ---------- lessons ----------

    @PostMapping("/lessons")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonContentResponse createLesson(@Valid @RequestBody CreateLessonRequest request) {
        return service.createLesson(request);
    }

    @PutMapping("/lessons/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public LessonContentResponse updateLesson(@PathVariable UUID id,
                                              @Valid @RequestBody UpdateLessonRequest request) {
        return service.updateLesson(id, request);
    }

    @DeleteMapping("/lessons/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLesson(@PathVariable UUID id) {
        service.deleteLesson(id);
    }

    @PatchMapping("/sections/{sectionId}/lessons/order")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderLessons(@PathVariable UUID sectionId, @Valid @RequestBody ReorderRequest request) {
        service.reorderLessons(sectionId, request);
    }

    @DeleteMapping("/lesson-files/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable UUID id) {
        service.deleteFile(id);
    }
}
