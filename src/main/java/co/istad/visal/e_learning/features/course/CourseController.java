package co.istad.visal.e_learning.features.course;

import co.istad.visal.e_learning.utils.PageResponse;
import co.istad.visal.e_learning.features.course.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Note where the checks live. Role gates sit here as annotations; ownership
 * is enforced inside the service through CourseAccessService. Putting
 * "is this mine" logic in a controller makes it impossible to reuse and easy
 * to forget on the next endpoint.
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    // ---------- public reads ----------

    @GetMapping
    public PageResponse<CourseSummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) CourseLevel level,
            @PageableDefault(size = 12) Pageable pageable) {

        return service.search(q, categoryId, level, pageable);
    }

    @GetMapping("/{id}")
    public CourseDetailResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping("/slug/{slug}")
    public CourseDetailResponse getBySlug(@PathVariable String slug) {
        return service.getBySlug(slug);
    }

    // ---------- instructor ----------

    @GetMapping("/mine")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public List<CourseSummaryResponse> mine() {
        return service.findMine();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDetailResponse create(@Valid @RequestBody CreateCourseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public CourseDetailResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateCourseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    /** Shared by both roles; the state machine decides which moves each may make. */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public CourseDetailResponse changeStatus(@PathVariable UUID id,
                                             @Valid @RequestBody ChangeStatusRequest request) {
        return service.changeStatus(id, request);
    }

    // ---------- admin ----------

    @GetMapping("/pending-review")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<CourseSummaryResponse> pendingReview() {
        return service.findPendingReview();
    }
}
