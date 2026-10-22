package co.istad.visal.e_learning.features.assistant;

import co.istad.visal.e_learning.features.assistant.dto.AskAssistantRequest;
import co.istad.visal.e_learning.features.assistant.dto.AskAssistantResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AssistantController {

    private final AssistantService service;

    public AssistantController(AssistantService service) {
        this.service = service;
    }

    /** Lets the UI hide or explain the tutor when no API key is configured. */
    @GetMapping("/assistant/status")
    public Map<String, Boolean> status() {
        return Map.of("enabled", service.isEnabled());
    }

    /** Tutor questions left for this lesson (3 per lesson by default). */
    @GetMapping("/lessons/{lessonId}/assistant")
    public co.istad.visal.e_learning.features.assistant.dto.TutorStatusResponse lessonStatus(@PathVariable UUID lessonId) {
        return service.status(lessonId);
    }

    /** Ask the AI tutor about a lesson the signed-in user can watch. */
    @PostMapping("/lessons/{lessonId}/assistant")
    public AskAssistantResponse ask(@PathVariable UUID lessonId, @Valid @RequestBody AskAssistantRequest request) {
        return service.ask(lessonId, request);
    }
}
