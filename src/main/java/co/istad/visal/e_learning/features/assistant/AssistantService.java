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
import co.istad.visal.e_learning.features.ai.AnswerGuard;
import co.istad.visal.e_learning.features.lessoncheck.LessonCheckResult;
import co.istad.visal.e_learning.features.lessoncheck.LessonCheckResultRepository;
import co.istad.visal.e_learning.features.lessoncheck.LessonCheckService;
import co.istad.visal.e_learning.features.lessoncheck.dto.CheckQuestion;
import co.istad.visal.e_learning.features.quiz.AnswerOption;
import co.istad.visal.e_learning.features.quiz.QuestionType;
import co.istad.visal.e_learning.features.quiz.QuizRepository;
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
    private static final int REWRITES = 2;
    private static final String QUIZ_QUESTION_RULE = """

            This is a question from the student's quiz. Do not state, spell out, split up or hint at the first letters of
            the answer, even if the lesson text contains it or the student insists. Explain the idea around it in other
            words, and let them work it out.
            """;

    private final LessonRepository lessonRepository;
    private final LessonBlocks lessonBlocks;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;
    private final GroqClient ai;
    private final AiRateLimiter limiter;
    private final AiHintUsageRepository usage;
    private final AiAccessGrantRepository grants;
    private final LessonCheckService lessonChecks;
    private final LessonCheckResultRepository checkResults;
    private final QuizRepository quizRepository;
    /** Tutor questions each learner may ask per lesson. */
    private final int questionsPerLesson;

    public AssistantService(LessonRepository lessonRepository, LessonBlocks lessonBlocks, CourseAccessService access,
                            CurrentUserService currentUser, GroqClient ai, AiRateLimiter limiter, AiHintUsageRepository usage,
                            AiAccessGrantRepository grants, LessonCheckService lessonChecks,
                            LessonCheckResultRepository checkResults, QuizRepository quizRepository,
                            @Value("${ai.tutor.questions-per-lesson:3}") int questionsPerLesson) {
        this.grants = grants;
        this.lessonChecks = lessonChecks;
        this.checkResults = checkResults;
        this.quizRepository = quizRepository;
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

        String asked = turns.get(turns.size() - 1).content();
        List<QuizItem> quizzes = quizItems(lesson);
        boolean isQuizQuestion = quizzes.stream().anyMatch(q -> AnswerGuard.asks(asked, q.question()));
        List<String> protectedAnswers = protectedAnswers(user, lesson, quizzes, asked);

        String system = systemPrompt(lesson, request.language()) + (isQuizQuestion ? QUIZ_QUESTION_RULE : "");
        String reply = ai.chat(system, history, 1500, 0.5);
        // Safety net: a reply that gives a quiz answer away is rewritten once, then replaced
        for (int attempt = 0; attempt < REWRITES && AnswerGuard.leaks(reply, protectedAnswers, ""); attempt++) {
            reply = ai.chat(system + QUIZ_QUESTION_RULE + "\nYour previous draft gave a quiz answer away. Rewrite it: still explain "
                    + "the idea helpfully, but do not use any of these words: " + AnswerGuard.forbiddenWords(protectedAnswers, "") + ".", history, 1500, 0.3);
        }
        if (AnswerGuard.leaks(reply, protectedAnswers, "")) {
            // Bunny did not really answer, so this one does not use up a question
            return new AskAssistantResponse(quizDeflection(request.language()), Math.max(0, limit - used));
        }

        // count only answered questions
        AiHintUsage row = usage.findByUserIdAndScopeAndRefIdAndAttemptNo(user.getId(), AiHintUsageRepository.TUTOR, lessonId, 0)
                .orElseGet(() -> new AiHintUsage(user.getId(), AiHintUsageRepository.TUTOR, lessonId, 0));
        row.increment();
        usage.save(row);
        return new AskAssistantResponse(reply, Math.max(0, limit - used - 1));
    }

    /** A question from one of this course's quizzes, with the answers that must not be said for it. */
    private record QuizItem(UUID lessonId, String question, List<String> answers) {
    }

    private List<QuizItem> quizItems(Lesson lesson) {
        Course course = lesson.getSection().getCourse();
        List<QuizItem> items = new ArrayList<>();
        for (Lesson l : lessonRepository.findAllByCourse(course.getId())) {
            for (CheckQuestion q : lessonChecks.questionsWithAnswers(l.getId())) {
                List<String> answers = q.type() == QuestionType.SHORT_ANSWER
                        ? (q.acceptedAnswers() == null ? List.of() : q.acceptedAnswers())
                        : q.type() == QuestionType.TRUE_FALSE || q.options() == null ? List.of()
                        : q.options().stream().filter(o -> Boolean.TRUE.equals(o.correct())).map(CheckQuestion.Option::text).toList();
                items.add(new QuizItem(l.getId(), q.text(), answers));
            }
        }
        quizRepository.findByCourseId(course.getId()).ifPresent(quiz -> quiz.getQuestions().forEach(q -> items.add(new QuizItem(null, q.getText(),
                q.getType() == QuestionType.SHORT_ANSWER ? q.acceptedAnswers()
                        : q.getType() == QuestionType.TRUE_FALSE ? List.of()
                        : q.getOptions().stream().filter(AnswerOption::isCorrect).map(AnswerOption::getText).toList()))));
        return items;
    }

    /**
     * What this reply must not say: the answers to any quiz question the student
     * just asked, plus the longer answers of this lesson's own quiz until they
     * have passed it, so "what is HTML?" cannot be used to get around a quiz on
     * what HTML stands for. Once passed, the lesson can be discussed freely.
     */
    private List<String> protectedAnswers(User user, Lesson lesson, List<QuizItem> quizzes, String asked) {
        boolean passed = checkResults.findByUserIdAndLessonId(user.getId(), lesson.getId())
                .map(LessonCheckResult::isPassed).orElse(false);
        List<String> answers = new ArrayList<>();
        for (QuizItem item : quizzes) {
            if (AnswerGuard.asks(asked, item.question())) {
                answers.addAll(item.answers());
            } else if (!passed && lesson.getId().equals(item.lessonId())) {
                item.answers().stream().filter(AnswerGuard::isDistinctive).forEach(answers::add);
            }
        }
        return answers;
    }

    private static String quizDeflection(String language) {
        return "km".equals(language)
                ? "សំណួរនេះជាផ្នែកមួយនៃកម្រងសំណួររបស់អ្នក ដូច្នេះ Bunny មិនអាចប្រាប់ចម្លើយបានទេ។ សាកល្បងសុំគន្លឹះនៅក្នុងកម្រងសំណួរ ឬអានផ្នែកនេះនៃមេរៀនម្តងទៀត។"
                : "That one's part of your quiz, so I can't give you the answer. Try asking for a hint in the quiz, or go back over that part of the lesson.";
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
