package co.istad.visal.e_learning.features.lessoncheck;

import co.istad.visal.e_learning.features.lessoncheck.dto.LessonCheckDtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Lesson checks (a short quiz per lesson).
 *   GET  /lessons/{id}/check        student: questions without answers + my last score
 *   POST /lessons/{id}/check        student: submit answers → graded result
 *   GET  /lessons/{id}/check/edit   instructor/admin: questions with answers
 *   PUT  /lessons/{id}/check        instructor/admin: replace all questions
 */
@RestController
@RequestMapping("/api/v1/lessons/{lessonId}/check")
public class LessonCheckController {

    private final LessonCheckService service;

    public LessonCheckController(LessonCheckService service) {
        this.service = service;
    }

    @GetMapping
    public LessonCheckResponse take(@PathVariable UUID lessonId) {
        return service.forStudent(lessonId);
    }

    @PostMapping
    public CheckResultResponse submit(@PathVariable UUID lessonId, @Valid @RequestBody SubmitCheckRequest request) {
        return service.submit(lessonId, request);
    }

    @GetMapping("/edit")
    public LessonCheckResponse forEditing(@PathVariable UUID lessonId) {
        return service.forEditing(lessonId);
    }

    @PutMapping
    public LessonCheckResponse save(@PathVariable UUID lessonId, @Valid @RequestBody SaveLessonCheckRequest request) {
        return service.save(lessonId, request);
    }
}
