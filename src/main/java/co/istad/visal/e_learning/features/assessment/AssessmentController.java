package co.istad.visal.e_learning.features.assessment;

import co.istad.visal.e_learning.features.assessment.dto.AssessmentDetailResponse;
import co.istad.visal.e_learning.features.assessment.dto.AssessmentSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Quiz results dashboard for teaching staff. Role here; ownership in the service. */
@RestController
@RequestMapping("/api/v1/assessments")
@PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
public class AssessmentController {

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<AssessmentSummaryResponse> list() {
        return service.list();
    }

    @GetMapping("/{courseId}")
    public AssessmentDetailResponse detail(@PathVariable UUID courseId) {
        return service.detail(courseId);
    }
}
