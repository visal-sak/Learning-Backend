package co.istad.visal.e_learning.features.ai;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.ai.dto.AiHelperDtos.*;
import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonBlocks;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.enrollment.Enrollment;
import co.istad.visal.e_learning.features.lessoncheck.LessonCheckResultRepository;
import co.istad.visal.e_learning.features.lessoncheck.LessonCheckService;
import co.istad.visal.e_learning.features.lessoncheck.dto.CheckQuestion;
import co.istad.visal.e_learning.features.quiz.AnswerOption;
import co.istad.visal.e_learning.features.quiz.Question;
import co.istad.visal.e_learning.features.quiz.QuestionType;
import co.istad.visal.e_learning.features.quiz.Quiz;
import co.istad.visal.e_learning.features.quiz.QuizAttemptRepository;
import co.istad.visal.e_learning.features.quiz.QuizRepository;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AI helper inside quizzes, controlled per course by the instructor:
 *  - hints while answering (a limited number per attempt; lesson quizzes, and the final quiz only if allowed)
 *  - "Explain my answer" after the quiz has been submitted (unlimited)
 * Hints never reveal the answer. Hint counts live on the server, per attempt.
 */
@Service
@Transactional(readOnly = true)
public class AiHelperService {

    private final GroqClient ai;
    private final AiRateLimiter limiter;
    private final AiHintUsageRepository usage;
    private final LessonRepository lessons;
    private final LessonBlocks lessonBlocks;
    private final LessonCheckService lessonChecks;
    private final LessonCheckResultRepository checkResults;
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;

    public AiHelperService(GroqClient ai, AiRateLimiter limiter, AiHintUsageRepository usage, LessonRepository lessons,
                           LessonBlocks lessonBlocks, LessonCheckService lessonChecks, LessonCheckResultRepository checkResults,
                           QuizRepository quizzes, QuizAttemptRepository attempts, CourseAccessService access,
                           CurrentUserService currentUser) {
        this.ai = ai;
        this.limiter = limiter;
        this.usage = usage;
        this.lessons = lessons;
        this.lessonBlocks = lessonBlocks;
        this.lessonChecks = lessonChecks;
        this.checkResults = checkResults;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.access = access;
        this.currentUser = currentUser;
    }

    // ------------------------------------------------------------------ lesson quizzes

    public AiHelperStatus lessonStatus(UUID lessonId) {
        User user = currentUser.require();
        Lesson lesson = loadLesson(lessonId);
        access.requireCanViewContent(lesson);
        Course course = lesson.getSection().getCourse();
        int used = usage.used(user.getId(), AiHintUsageRepository.LESSON_CHECK, lessonId, nextLessonAttempt(user, lessonId));
        return status(course, course.isAiHelperEnabled(), used);
    }

    @Transactional
    public HintResponse lessonHint(UUID lessonId, HintRequest request) {
        User user = currentUser.require();
        Lesson lesson = loadLesson(lessonId);
        access.requireCanViewContent(lesson);
        Course course = lesson.getSection().getCourse();
        if (!course.isAiHelperEnabled()) throw new ConflictException("The AI helper is turned off for this course");

        CheckQuestion q = lessonChecks.questionsWithAnswers(lessonId).stream()
                .filter(x -> x.id().equals(request.questionId())).findFirst()
                .orElseThrow(() -> new NotFoundException("No question with id " + request.questionId()));

        int attemptNo = nextLessonAttempt(user, lessonId);
        String hint = giveHint(user, course, AiHintUsageRepository.LESSON_CHECK, lessonId, attemptNo,
                lesson.getTitle(), lessonText(lesson), q.text(), optionTexts(q), request.language());
        int used = usage.used(user.getId(), AiHintUsageRepository.LESSON_CHECK, lessonId, attemptNo);
        return new HintResponse(hint, used, Math.max(0, course.getAiHintsPerAttempt() - used));
    }

    public ExplainResponse lessonExplain(UUID lessonId, ExplainRequest request) {
        User user = currentUser.require();
        Lesson lesson = loadLesson(lessonId);
        access.requireCanViewContent(lesson);
        Course course = lesson.getSection().getCourse();
        if (!course.isAiExplainEnabled()) throw new ConflictException("Explanations are turned off for this course");
        if (nextLessonAttempt(user, lessonId) == 1) throw new ConflictException("Submit the quiz first, then ask for an explanation");

        CheckQuestion q = lessonChecks.questionsWithAnswers(lessonId).stream()
                .filter(x -> x.id().equals(request.questionId())).findFirst()
                .orElseThrow(() -> new NotFoundException("No question with id " + request.questionId()));

        List<String> options = optionTexts(q);
        List<String> correct = q.options() == null ? List.of()
                : q.options().stream().filter(o -> Boolean.TRUE.equals(o.correct())).map(CheckQuestion.Option::text).toList();
        String studentAnswer = q.type() == QuestionType.SHORT_ANSWER
                ? nz(request.text())
                : (request.selected() == null ? List.<Integer>of() : request.selected()).stream()
                        .filter(i -> i != null && i >= 0 && i < options.size()).map(options::get).collect(Collectors.joining(", "));

        limiter.check(user.getId());
        return new ExplainResponse(explain(lesson.getTitle(), lessonText(lesson), q.text(), options, correct,
                q.acceptedAnswers(), studentAnswer, q.explanation(), request.language()));
    }

    // ------------------------------------------------------------------ final quiz

    public AiHelperStatus quizStatus(UUID quizId) {
        User user = currentUser.require();
        Quiz quiz = loadQuiz(quizId);
        Enrollment enrollment = access.requireEnrollment(quiz.getCourse());
        Course course = quiz.getCourse();
        int used = usage.used(user.getId(), AiHintUsageRepository.FINAL_QUIZ, quizId, nextQuizAttempt(enrollment, quizId));
        return status(course, course.isAiHelperEnabled() && course.isAiInFinalQuiz(), used);
    }

    @Transactional
    public HintResponse quizHint(UUID quizId, HintRequest request) {
        User user = currentUser.require();
        Quiz quiz = loadQuiz(quizId);
        Enrollment enrollment = access.requireEnrollment(quiz.getCourse());
        Course course = quiz.getCourse();
        if (!course.isAiHelperEnabled() || !course.isAiInFinalQuiz()) {
            throw new ConflictException("The AI helper is not allowed in the final quiz");
        }
        Question q = findQuestion(quiz, request.questionId());
        int attemptNo = nextQuizAttempt(enrollment, quizId);
        String hint = giveHint(user, course, AiHintUsageRepository.FINAL_QUIZ, quizId, attemptNo,
                quiz.getTitle(), "", q.getText(), q.getOptions().stream().map(AnswerOption::getText).toList(), request.language());
        int used = usage.used(user.getId(), AiHintUsageRepository.FINAL_QUIZ, quizId, attemptNo);
        return new HintResponse(hint, used, Math.max(0, course.getAiHintsPerAttempt() - used));
    }

    public ExplainResponse quizExplain(UUID quizId, ExplainRequest request) {
        User user = currentUser.require();
        Quiz quiz = loadQuiz(quizId);
        Enrollment enrollment = access.requireEnrollment(quiz.getCourse());
        Course course = quiz.getCourse();
        if (!course.isAiExplainEnabled()) throw new ConflictException("Explanations are turned off for this course");
        if (nextQuizAttempt(enrollment, quizId) == 1) throw new ConflictException("Submit the quiz first, then ask for an explanation");

        Question q = findQuestion(quiz, request.questionId());
        List<String> options = q.getOptions().stream().map(AnswerOption::getText).toList();
        List<String> correct = q.getOptions().stream().filter(AnswerOption::isCorrect).map(AnswerOption::getText).toList();
        Set<UUID> picked = request.selectedOptionIds() == null ? Set.of() : Set.copyOf(request.selectedOptionIds());
        String studentAnswer = q.getType() == QuestionType.SHORT_ANSWER
                ? nz(request.text())
                : q.getOptions().stream().filter(o -> picked.contains(o.getId())).map(AnswerOption::getText).collect(Collectors.joining(", "));

        limiter.check(user.getId());
        return new ExplainResponse(explain(quiz.getTitle(), "", q.getText(), options, correct, q.acceptedAnswers(),
                studentAnswer, q.getExplanation(), request.language()));
    }

    // ------------------------------------------------------------------ shared

    private AiHelperStatus status(Course course, boolean hintsOn, int used) {
        int perAttempt = course.getAiHintsPerAttempt();
        boolean hints = ai.isEnabled() && hintsOn && perAttempt > 0;
        return new AiHelperStatus(ai.isEnabled(), hints, perAttempt, used, Math.max(0, perAttempt - used),
                ai.isEnabled() && course.isAiExplainEnabled(), course.getAiHintPenalty());
    }

    private String giveHint(User user, Course course, String scope, UUID refId, int attemptNo, String title, String context,
                            String question, List<String> options, String language) {
        int used = usage.used(user.getId(), scope, refId, attemptNo);
        if (used >= course.getAiHintsPerAttempt()) {
            throw new ConflictException("You've used all your hints for this attempt");
        }
        limiter.check(user.getId());

        String system = """
                You are a supportive tutor helping a student who is stuck on ONE quiz question.
                Give a single short hint (at most 3 sentences) that points them to the right idea.
                Never state the correct answer, never say which option is right or wrong, and never eliminate options.
                Ask a guiding question or remind them of the concept instead.
                %s""".formatted(languageRule(language));
        String prompt = "Lesson / quiz: " + title + "\n"
                + (context.isBlank() ? "" : "Lesson notes:\n" + context + "\n")
                + "Question: " + question + "\n"
                + (options.isEmpty() ? "(The student types a short answer.)" : "Options:\n" + numbered(options));

        String hint = ai.chat(system, List.of(GroqClient.Message.user(prompt)), 300, 0.4);

        AiHintUsage row = usage.findByUserIdAndScopeAndRefIdAndAttemptNo(user.getId(), scope, refId, attemptNo)
                .orElseGet(() -> new AiHintUsage(user.getId(), scope, refId, attemptNo));
        row.increment();
        usage.save(row);
        return hint;
    }

    private String explain(String title, String context, String question, List<String> options, List<String> correct,
                           List<String> accepted, String studentAnswer, String teacherNote, String language) {
        String system = """
                You explain quiz answers to a student after they submitted.
                In at most 120 words: say why the correct answer is correct, and if the student's answer differs,
                kindly explain what is wrong with it. Be clear and encouraging; use a tiny example if it helps.
                %s""".formatted(languageRule(language));
        String prompt = "Lesson / quiz: " + title + "\n"
                + (context.isBlank() ? "" : "Lesson notes:\n" + context + "\n")
                + "Question: " + question + "\n"
                + (options.isEmpty() ? "" : "Options:\n" + numbered(options) + "\n")
                + "Correct answer: " + (correct.isEmpty() ? String.join(" / ", accepted == null ? List.of() : accepted) : String.join(", ", correct)) + "\n"
                + "Student's answer: " + (studentAnswer.isBlank() ? "(no answer)" : studentAnswer) + "\n"
                + (teacherNote == null || teacherNote.isBlank() ? "" : "Instructor's note: " + teacherNote);
        return ai.chat(system, List.of(GroqClient.Message.user(prompt)), 500, 0.3);
    }

    private int nextLessonAttempt(User user, UUID lessonId) {
        return checkResults.findByUserIdAndLessonId(user.getId(), lessonId).map(r -> r.getAttempts() + 1).orElse(1);
    }

    private int nextQuizAttempt(Enrollment enrollment, UUID quizId) {
        return (int) attempts.countByEnrollmentIdAndQuizId(enrollment.getId(), quizId) + 1;
    }

    private Lesson loadLesson(UUID id) {
        return lessons.findWithCourse(id).orElseThrow(() -> new NotFoundException("No lesson with id " + id));
    }

    private Quiz loadQuiz(UUID id) {
        return quizzes.findById(id).orElseThrow(() -> new NotFoundException("No quiz with id " + id));
    }

    private static Question findQuestion(Quiz quiz, String id) {
        return quiz.getQuestions().stream().filter(q -> q.getId().toString().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("No question with id " + id));
    }

    private String lessonText(Lesson lesson) {
        String text = (nz(lesson.getContent()) + "\n\n" + lessonBlocks.toPlainText(lesson.getBlocks())).trim();
        return text.length() > 4000 ? text.substring(0, 4000) : text;
    }

    private static List<String> optionTexts(CheckQuestion q) {
        return q.options() == null ? List.of() : q.options().stream().map(CheckQuestion.Option::text).toList();
    }

    private static String numbered(List<String> options) {
        List<String> lines = new ArrayList<>();
        IntStream.range(0, options.size()).forEach(i -> lines.add((char) ('A' + i) + ". " + options.get(i)));
        return String.join("\n", lines);
    }

    private static String languageRule(String language) {
        return "km".equals(language)
                ? "Reply in Khmer (ភាសាខ្មែរ). Keep code and technical terms in English."
                : "Reply in English.";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
