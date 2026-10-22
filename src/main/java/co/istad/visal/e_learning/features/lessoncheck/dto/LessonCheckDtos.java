package co.istad.visal.e_learning.features.lessoncheck.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request / response shapes for lesson checks. */
public final class LessonCheckDtos {

    private LessonCheckDtos() {
    }

    /** Instructor saves the whole question list at once (empty list removes the check). */
    public record SaveLessonCheckRequest(
            @NotNull
            @Valid
            @Size(max = 30, message = "A lesson check can have at most 30 questions")
            List<CheckQuestion> questions,

            @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(100)
            Short passMark
    ) {
    }

    /** `myResult` is null for instructors and for students who haven't answered yet. */
    public record LessonCheckResponse(UUID lessonId, List<CheckQuestion> questions, MyResult myResult,
                                      Short passMark, boolean quizItem, boolean requirePass) {
    }

    public record MyResult(int correct, int total, int scorePercent, int attempts, Instant answeredAt, boolean passed) {
    }

    public record SubmitCheckRequest(
            @NotNull
            @Size(max = 30)
            List<Answer> answers
    ) {
        /** selected = option indexes (choice types); text = typed answer (short answer). */
        public record Answer(String questionId, List<Integer> selected, @Size(max = 200) String text) {
        }
    }

    /** progress is set when this submission completed a quiz item in the curriculum. */
    public record CheckResultResponse(int correct, int total, int scorePercent, List<QuestionResult> results,
                                      boolean passed, Short passMark,
                                      co.istad.visal.e_learning.features.enrollment.dto.ProgressResponse progress) {
    }

    public record QuestionResult(String questionId, boolean correct, List<Integer> correctOptions,
                                 List<String> acceptedAnswers, String explanation) {
    }
}
