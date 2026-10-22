package co.istad.visal.e_learning.features.ai;

import co.istad.visal.e_learning.features.ai.dto.AiAccessDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Extra Bunny AI help per student per lesson (admins: any course; instructors: their own).
 *   GET    /manage/ai-access?courseId=   grants on a course
 *   POST   /manage/ai-access             give extra questions / hints (+ notification, optional email)
 *   DELETE /manage/ai-access/{id}        take a grant back
 */
@RestController
@RequestMapping("/api/v1/manage/ai-access")
@PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
public class AiAccessController {

    private final AiAccessService service;

    public AiAccessController(AiAccessService service) {
        this.service = service;
    }

    @GetMapping
    public List<AiAccessGrantResponse> forCourse(@RequestParam UUID courseId) {
        return service.forCourse(courseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<AiAccessGrantResponse> grant(@Valid @RequestBody GrantAiAccessRequest request) {
        return service.grant(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id) {
        service.revoke(id);
    }
}
