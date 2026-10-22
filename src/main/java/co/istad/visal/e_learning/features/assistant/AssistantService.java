package co.istad.visal.e_learning.features.assistant;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.expection.ServiceUnavailableException;
import co.istad.visal.e_learning.features.assistant.dto.AskAssistantRequest;
import co.istad.visal.e_learning.features.assistant.dto.AskAssistantResponse;
import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonBlocks;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.beta.messages.BetaCacheControlEphemeral;
import com.anthropic.models.beta.messages.BetaContentBlock;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaTextBlockParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI lesson tutor. Answers a student's questions about the lesson they are
 * watching, grounded in that lesson's text, in English or Khmer.
 *
 * The API key is read from the server environment (ANTHROPIC_API_KEY) and never
 * leaves the backend. Without a key the endpoint answers 503 with a clear message.
 */
@Service
@Transactional(readOnly = true)
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    /** Keep only the most recent turns — enough context for a follow-up, bounded cost. */
    private static final int MAX_TURNS = 12;
    /** Per-student cap: requests per window, to keep a runaway client from running up the bill. */
    private static final int RATE_LIMIT = 20;
    private static final Duration RATE_WINDOW = Duration.ofMinutes(10);
    /** Lesson text placed in the prompt is capped so one huge lesson can't blow up the request. */
    private static final int MAX_LESSON_CHARS = 60_000;

    private final LessonRepository lessonRepository;
    private final LessonBlocks lessonBlocks;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;
    private final AnthropicClient client; // null when no API key is configured
    private final String model;
    private final Map<UUID, Deque<Instant>> recentRequests = new ConcurrentHashMap<>();

    public AssistantService(LessonRepository lessonRepository,
                            LessonBlocks lessonBlocks,
                            CourseAccessService access,
                            CurrentUserService currentUser,
                            @Value("${anthropic.api-key:}") String apiKey,
                            @Value("${anthropic.model:claude-opus-5-5}") String model) {
        this.lessonRepository = lessonRepository;
        this.lessonBlocks = lessonBlocks;
        this.access = access;
        this.currentUser = currentUser;
        this.model = model;
        this.client = apiKey == null || apiKey.isBlank() ? null : AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        if (client == null) log.warn("AI assistant disabled: set ANTHROPIC_API_KEY to enable it");
    }

    public boolean isEnabled() {
        return client != null;
    }

    public AskAssistantResponse ask(UUID lessonId, AskAssistantRequest request) {
        if (client == null) {
            throw new ServiceUnavailableException("The AI assistant isn't set up yet. Ask an admin to add an API key.");
        }
        User user = currentUser.require();
        Lesson lesson = lessonRepository.findWithCourse(lessonId)
                .orElseThrow(() -> new NotFoundException("No lesson with id " + lessonId));
        access.requireCanViewContent(lesson);
        checkRateLimit(user.getId());

        List<AskAssistantRequest.Turn> turns = request.messages();
        if (!"user".equals(turns.get(turns.size() - 1).role())) {
            throw new ConflictException("The last message must be the student's question");
        }
        List<AskAssistantRequest.Turn> recent = turns.subList(Math.max(0, turns.size() - MAX_TURNS), turns.size());

        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(4096L)
                // Lesson context is the stable prefix — cached so follow-up questions are cheaper
                .systemOfBetaTextBlockParams(List.of(BetaTextBlockParam.builder()
                        .text(systemPrompt(lesson, request.language()))
                        .cacheControl(BetaCacheControlEphemeral.builder().build())
                        .build()))
                // If a request is declined by a safety classifier, retry it on a fallback model
                .addBeta("server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));

        // History must start with a user turn; drop any leading assistant turns
        boolean started = false;
        for (AskAssistantRequest.Turn t : recent) {
            if (!started && !"user".equals(t.role())) continue;
            started = true;
            if ("user".equals(t.role())) params.addUserMessage(t.content());
            else params.addAssistantMessage(t.content());
        }

        try {
            BetaMessage response = client.beta().messages().create(params.build());
            if (response.stopReason().map(BetaStopReason.REFUSAL::equals).orElse(false)) {
                return new AskAssistantResponse("km".equals(request.language())
                        ? "សូមអភ័យទោស ខ្ញុំមិនអាចជួយសំណួរនេះបានទេ។ សូមសួរអំពីមេរៀននេះ។"
                        : "Sorry, I can't help with that one. Try asking about this lesson.");
            }
            String reply = response.content().stream()
                    .map(BetaContentBlock::text)
                    .flatMap(java.util.Optional::stream)
                    .map(b -> b.text())
                    .collect(Collectors.joining("\n\n"))
                    .trim();
            return new AskAssistantResponse(reply.isEmpty() ? "…" : reply);
        } catch (RateLimitException e) {
            throw new ServiceUnavailableException("The AI assistant is busy right now. Try again in a minute.");
        } catch (AnthropicServiceException e) {
            log.warn("AI assistant call failed: {}", e.getMessage());
            throw new ServiceUnavailableException("The AI assistant couldn't answer right now. Try again shortly.");
        }
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
                - %s
                """.formatted(course.getTitle(), lesson.getSection().getTitle(), lesson.getTitle(), content, reply);
    }

    private void checkRateLimit(UUID userId) {
        Instant now = Instant.now();
        Deque<Instant> times = recentRequests.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(RATE_WINDOW))) times.pollFirst();
            if (times.size() >= RATE_LIMIT) {
                throw new ConflictException("You've asked a lot of questions in a short time. Please wait a few minutes.");
            }
            times.addLast(now);
        }
    }
}
