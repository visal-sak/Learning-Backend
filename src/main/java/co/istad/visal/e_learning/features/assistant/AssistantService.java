package co.istad.visal.e_learning.features.assistant;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.ai.AiHintUsage;
import co.istad.visal.e_learning.features.ai.AiHintUsageRepository;
import co.istad.visal.e_learning.features.ai.AiAccessGrantRepository;
import co.istad.visal.e_learning.features.ai.AiRateLimiter;
import co.istad.visal.e_learning.features.assistant.dto.TutorStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import co.istad.visal.e_learning.features.ai.GroqClient;
import co.istad.visal.e_learning.features.assistant.dto.AskAssistantRequest;
import co.istad.visal.e_learning.features.assistant.dto.AskAssistantResponse;
import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonBlocks;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI lesson tutor (Groq). Answers a learner's questions about the lesson they are watching,
 * grounded in that lesson's text, in English or Khmer. Open to anyone who can view the lesson —
 * students, and instructors / admins on their courses.
 */
@Service
@Transactional(readOnly = true)
public class AssistantService {

    /** Keep only the most recent turns — enough context for a follow-up, bounded cost. */
    private static final int MAX_TURNS = 12;
    /** Lesson text placed in the prompt is capped so one huge lesson can't blow up the request. */
    private static final int MAX_LESSON_CHARS = 20_000;

    private final LessonRepository lessonRepository;
    private final LessonBlocks lessonBlocks;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;
    private final GroqClient ai;
    private final AiRateLimiter limiter;
    private final AiHintUsageRepository usage;
    private final AiAccessGrantRepository grants;
    /** Tutor questions each learner may ask per lesson. */
    private final int questionsPerLesson;

    public AssistantService(LessonRepository lessonRepository, LessonBlocks lessonBlocks, CourseAccessService access,
                            CurrentUserService currentUser, GroqClient ai, AiRateLimiter limiter, AiHintUsageRepository usage,
                            AiAccessGrantRepository grants,
                            @Value("${ai.tutor.questions-per-lesson:3}") int questionsPerLesson) {
        this.grants = grants;
        this.usage = usage;
        this.questionsPerLesson = questionsPerLesson;
        this.lessonRepository = lessonRepository;
        this.lessonBlocks = lessonBlocks;
        this.access = access;
        this.currentUser = currentUser;
        this.ai = ai;
        this.limiter = limiter;
    }

    public boolean isEnabled() {
        return ai.isEnabled();
    }

    /** How many tutor questions the signed-in learner has left for this lesson. */
    public TutorStatusResponse status(UUID lessonId) {
        User user = currentUser.require();
        Lesson lesson = lessonRepository.findWithCourse(lessonId)
                .orElseThrow(() -> new NotFoundException("No lesson with id " + lessonId));
        access.requireCanViewContent(lesson);
        int used = usage.used(user.getId(), AiHintUsageRepository.TUTOR, lessonId, 0);
        int extra = grants.extraQuestions(user.getId(), lessonId);
        int limit = questionsPerLesson + extra;
        return new TutorStatusResponse(ai.isEnabled(), limit, used, Math.max(0, limit - used), extra);
    }

    @Transactional
    public AskAssistantResponse ask(UUID lessonId, AskAssistantRequest request) {
        User user = currentUser.require();
        Lesson lesson = lessonRepository.findWithCourse(lessonId)
                .orElseThrow(() -> new NotFoundException("No lesson with id " + lessonId));
        access.requireCanViewContent(lesson);

        List<AskAssistantRequest.Turn> turns = request.messages();
        if (!"user".equals(turns.get(turns.size() - 1).role())) {
            throw new ConflictException("The last message must be the student's question");
        }
        int used = usage.used(user.getId(), AiHintUsageRepository.TUTOR, lessonId, 0);
        int limit = questionsPerLesson + grants.extraQuestions(user.getId(), lessonId);
        if (used >= limit) {
            throw new ConflictException("You've used all " + limit + " AI tutor questions for this lesson");
        }
        limiter.check(user.getId());

        // History must start with a user turn; drop any leading assistant turns
        List<GroqClient.Message> history = new ArrayList<>();
        for (AskAssistantRequest.Turn t : turns.subList(Math.max(0, turns.size() - MAX_TURNS), turns.size())) {
            if (history.isEmpty() && !"user".equals(t.role())) continue;
            history.add("user".equals(t.role()) ? GroqClient.Message.user(t.content()) : GroqClient.Message.assistant(t.content()));
        }

        String reply = ai.chat(systemPrompt(lesson, request.language()), history, 1500, 0.5);

        // count only answered questions
        AiHintUsage row = usage.findByUserIdAndScopeAndRefIdAndAttemptNo(user.getId(), AiHintUsageRepository.TUTOR, lessonId, 0)
                .orElseGet(() -> new AiHintUsage(user.getId(), AiHintUsageRepository.TUTOR, lessonId, 0));
        row.increment();
        usage.save(row);
        return new AskAssistantResponse(reply, Math.max(0, limit - used - 1));
    }

    private String systemPrompt(Lesson lesson, String language) {
        Course course = lesson.getSection().getCourse();
        String written = String.join("\n\n",
                lesson.getContent() == null ? "" : lesson.getContent(),
                lessonBlocks.toPlainText(lesson.getBlocks())).trim();
        String content = written.isEmpty()
                ? "(This lesson has no written text — it is a video lesson. Answer from general knowledge of the topic named in the title.)"
                : written.length() > MAX_LESSON_CHARS ? written.substring(0, MAX_LESSON_CHARS) : written;
        String reply = "km".equals(language)
                ? "Reply in Khmer (ភាសាខ្មែរ). Keep technical terms and code in English where that is clearer."
                : "Reply in English.";

        return """
                You are the friendly tutor inside Autumn, an online learning platform. A student is watching a lesson and asks you questions about it.

                Course: %s
                Section: %s
                Lesson: %s

                Lesson text:
                <lesson>
                %s
                </lesson>

                How to help:
                - Explain clearly and simply, for a learner. Prefer short answers with a small example; use code blocks for code.
                - Stay on this lesson and its course topic. If the question is unrelated, say so kindly and steer back to the lesson.
                - If the lesson text doesn't cover something, you may use general knowledge, but say it goes beyond the lesson.
                - If the student asks for quiz answers, help them understand the idea instead of just giving the answer.
                - Do not write quizzes, tests or practice questions, even if asked; offer a short summary or explanation instead.
                - %s
                """.formatted(course.getTitle(), lesson.getSection().getTitle(), lesson.getTitle(), content, reply);
    }
}
