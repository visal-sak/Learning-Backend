package co.istad.visal.e_learning.features.lessonfeedback;

import co.istad.visal.e_learning.features.lessonfeedback.dto.LessonFeedbackDtos.FeedbackResponse;
import co.istad.visal.e_learning.features.lessonfeedback.dto.LessonFeedbackDtos.MyFeedback;
import co.istad.visal.e_learning.features.lessonfeedback.dto.LessonFeedbackDtos.SaveFeedbackRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LessonFeedbackController {

    private final LessonFeedbackService service;

    public LessonFeedbackController(LessonFeedbackService service) {
        this.service = service;
    }

    @GetMapping("/lessons/{lessonId}/feedback/mine")
    public MyFeedback mine(@PathVariable UUID lessonId) {
        return service.mine(lessonId);
    }

    @PutMapping("/lessons/{lessonId}/feedback/mine")
    public MyFeedback save(@PathVariable UUID lessonId, @Valid @RequestBody SaveFeedbackRequest request) {
        return service.save(lessonId, request);
    }

    @GetMapping("/manage/lesson-feedback")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public List<FeedbackResponse> list(@RequestParam(required = false) UUID courseId) {
        return service.list(courseId);
    }

    @DeleteMapping("/manage/lesson-feedback/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
