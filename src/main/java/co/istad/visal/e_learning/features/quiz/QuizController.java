package co.istad.visal.e_learning.features.quiz;

import co.istad.visal.e_learning.features.quiz.dto.instructor.*;
import co.istad.visal.e_learning.features.quiz.dto.student.AttemptResultResponse;
import co.istad.visal.e_learning.features.quiz.dto.student.QuizTakeResponse;
import co.istad.visal.e_learning.features.quiz.dto.student.SubmitAttemptRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * hasAnyAuthority, never hasAnyRole.
 *
 * JwtRoleConverter maps realm roles with SimpleGrantedAuthority::new, so the
 * authority is plain "ADMIN". hasRole silently prepends ROLE_ before
 * comparing, so it can never match — the URL matcher lets the request
 * through and then method security rejects it with a 403, for a user who
 * visibly holds the role.
 */
@RestController
@RequestMapping("/api/v1")
public class QuizController {

    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    // ---------- authoring ----------

    @GetMapping("/quizzes/{id}/edit")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public QuizDetailResponse forEditing(@PathVariable UUID id) {
        return service.getForEditing(id);
    }

    // One quiz per course: editors look it up by course instead of remembering its id
    @GetMapping("/courses/{courseId}/quiz/edit")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public QuizDetailResponse forEditingByCourse(@PathVariable UUID courseId) {
        return service.getForEditingByCourse(courseId);
    }

    @PostMapping("/quizzes")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizDetailResponse create(@Valid @RequestBody CreateQuizRequest request) {
        return service.create(request);
    }

    @PutMapping("/quizzes/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public QuizDetailResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateQuizRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/quizzes/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/questions")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse addQuestion(@Valid @RequestBody SaveQuestionRequest request) {
        return service.saveQuestion(request);
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public QuestionResponse updateQuestion(@PathVariable UUID id,
                                           @Valid @RequestBody SaveQuestionRequest request) {
        return service.updateQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable UUID id) {
        service.deleteQuestion(id);
    }

    /**
     * Everybody's attempts. One path segment away from /my-attempts below,
     * and the opposite audience — which is why both need explicit rules in
     * SecurityConfig too.
     */
    @GetMapping("/quizzes/{id}/attempts")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public List<AttemptResultResponse> allAttempts(@PathVariable UUID id) {
        return service.findAllAttempts(id);
    }

    // ---------- taking ----------

    /**
     * No @PreAuthorize on any of these three. "Enrolled in this course" is
     * not a role, so the check happens in the service against the enrollment
     * table. A role annotation here would either lock out real students or
     * wave through everyone.
     */
    @GetMapping("/courses/{courseId}/quiz/take")
    public QuizTakeResponse take(@PathVariable UUID courseId) {
        return service.startAttempt(courseId);
    }

    @PostMapping("/quizzes/{id}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptResultResponse submit(@PathVariable UUID id,
                                        @Valid @RequestBody SubmitAttemptRequest request) {
        return service.submit(id, request);
    }

    @GetMapping("/quizzes/{id}/my-attempts")
    public List<AttemptResultResponse> myAttempts(@PathVariable UUID id) {
        return service.findMyAttempts(id);
    }
}