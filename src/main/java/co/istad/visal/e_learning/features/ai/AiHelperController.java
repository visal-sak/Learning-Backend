package co.istad.visal.e_learning.features.ai;

import co.istad.visal.e_learning.features.ai.dto.AiHelperDtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * AI helper in quizzes (signed-in learners with access; instructors and admins can try it on their courses).
 *   GET  /ai-helper/lesson-checks/{lessonId}           what the lesson quiz may offer (hints left, explain on/off)
 *   POST /ai-helper/lesson-checks/{lessonId}/hint      a hint for one question (counts toward the attempt's limit)
 *   POST /ai-helper/lesson-checks/{lessonId}/explain   explain my answer (after submitting)
 *   … the same three under /ai-helper/quizzes/{quizId} for the final quiz
 */
@RestController
@RequestMapping("/api/v1/ai-helper")
public class AiHelperController {

    private final AiHelperService service;

    public AiHelperController(AiHelperService service) {
        this.service = service;
    }

    @GetMapping("/lesson-checks/{lessonId}")
    public AiHelperStatus lessonStatus(@PathVariable UUID lessonId) {
        return service.lessonStatus(lessonId);
    }

    @PostMapping("/lesson-checks/{lessonId}/hint")
    public HintResponse lessonHint(@PathVariable UUID lessonId, @Valid @RequestBody HintRequest request) {
        return service.lessonHint(lessonId, request);
    }

    @PostMapping("/lesson-checks/{lessonId}/explain")
    public ExplainResponse lessonExplain(@PathVariable UUID lessonId, @Valid @RequestBody ExplainRequest request) {
        return service.lessonExplain(lessonId, request);
    }

    @GetMapping("/quizzes/{quizId}")
    public AiHelperStatus quizStatus(@PathVariable UUID quizId) {
        return service.quizStatus(quizId);
    }

    @PostMapping("/quizzes/{quizId}/hint")
    public HintResponse quizHint(@PathVariable UUID quizId, @Valid @RequestBody HintRequest request) {
        return service.quizHint(quizId, request);
    }

    @PostMapping("/quizzes/{quizId}/explain")
    public ExplainResponse quizExplain(@PathVariable UUID quizId, @Valid @RequestBody ExplainRequest request) {
        return service.quizExplain(quizId, request);
    }
}
