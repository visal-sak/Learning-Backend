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
    private final AiAccessGrantRepository grants;

    public AiHelperService(GroqClient ai, AiRateLimiter limiter, AiHintUsageRepository usage, LessonRepository lessons,
                           LessonBlocks lessonBlocks, LessonCheckService lessonChecks, LessonCheckResultRepository checkResults,
                           QuizRepository quizzes, QuizAttemptRepository attempts, CourseAccessService access,
                           CurrentUserService currentUser, AiAccessGrantRepository grants) {
        this.grants = grants;
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
        int used = usage.totalUsed(user.getId(), AiHintUsageRepository.LESSON_CHECK, lessonId);
        return status(course, course.isAiHelperEnabled(), used, grants.extraHints(user.getId(), lessonId));
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

        List<String> options = optionTexts(q);
        List<String> correct = q.options() == null ? List.of()
                : q.options().stream().filter(o -> Boolean.TRUE.equals(o.correct())).map(CheckQuestion.Option::text).toList();
        String current = q.type() == QuestionType.SHORT_ANSWER ? nz(request.text()) : picked(options, request.selected());

        int attemptNo = nextLessonAttempt(user, lessonId);
        int limit = course.getAiHintsPerAttempt() + grants.extraHints(user.getId(), lessonId);
        String hint = giveHint(user, limit, AiHintUsageRepository.LESSON_CHECK, lessonId, attemptNo, lesson.getTitle(), lessonText(lesson),
                new HintInput(q.type(), q.text(), options, correct, q.acceptedAnswers(), current, request.previousHints()), request.language());
        int used = usage.totalUsed(user.getId(), AiHintUsageRepository.LESSON_CHECK, lessonId);
        return new HintResponse(hint, used, Math.max(0, limit - used));
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
        String studentAnswer = q.type() == QuestionType.SHORT_ANSWER ? nz(request.text()) : picked(options, request.selected());

        limiter.check(user.getId());
        return new ExplainResponse(explain(lesson.getTitle(), lessonText(lesson), q.type(), q.text(), options, correct,
                q.acceptedAnswers(), studentAnswer, q.explanation(), request.language()));
    }

    // ------------------------------------------------------------------ final quiz

    public AiHelperStatus quizStatus(UUID quizId) {
        User user = currentUser.require();
        Quiz quiz = loadQuiz(quizId);
        Enrollment enrollment = access.requireEnrollment(quiz.getCourse());
        Course course = quiz.getCourse();
        int used = usage.totalUsed(user.getId(), AiHintUsageRepository.FINAL_QUIZ, quizId);
        return status(course, course.isAiHelperEnabled() && course.isAiInFinalQuiz(), used, 0);
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
        List<String> options = q.getOptions().stream().map(AnswerOption::getText).toList();
        List<String> correct = q.getOptions().stream().filter(AnswerOption::isCorrect).map(AnswerOption::getText).toList();
        Set<UUID> chosen = request.selectedOptionIds() == null ? Set.of() : Set.copyOf(request.selectedOptionIds());
        String current = q.getType() == QuestionType.SHORT_ANSWER ? nz(request.text())
                : q.getOptions().stream().filter(o -> chosen.contains(o.getId())).map(AnswerOption::getText).collect(Collectors.joining(", "));

        int attemptNo = nextQuizAttempt(enrollment, quizId);
        String hint = giveHint(user, course.getAiHintsPerAttempt(), AiHintUsageRepository.FINAL_QUIZ, quizId, attemptNo, quiz.getTitle(), "",
                new HintInput(q.getType(), q.getText(), options, correct, q.acceptedAnswers(), current, request.previousHints()), request.language());
        int used = usage.totalUsed(user.getId(), AiHintUsageRepository.FINAL_QUIZ, quizId);
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
        return new ExplainResponse(explain(quiz.getTitle(), "", q.getType(), q.getText(), options, correct, q.acceptedAnswers(),
                studentAnswer, q.getExplanation(), request.language()));
    }

    // ------------------------------------------------------------------ shared

    private AiHelperStatus status(Course course, boolean hintsOn, int used, int extra) {
        int perAttempt = course.getAiHintsPerAttempt() + extra;
        boolean hints = ai.isEnabled() && hintsOn && perAttempt > 0;
        return new AiHelperStatus(ai.isEnabled(), hints, perAttempt, used, Math.max(0, perAttempt - used),
                ai.isEnabled() && course.isAiExplainEnabled(), course.getAiHintPenalty(), extra);
    }

    /** Everything the model needs for an accurate hint. correct / accepted are secret: used to aim the hint, never shown. */
    private record HintInput(QuestionType type, String question, List<String> options, List<String> correct,
                             List<String> accepted, String currentAnswer, List<String> previousHints) {
    }

    private String giveHint(User user, int limit, String scope, UUID refId, int attemptNo, String title, String context,
                            HintInput in, String language) {
        int used = usage.totalUsed(user.getId(), scope, refId);
        if (used >= limit) {
            throw new ConflictException("You've used all your hints for this attempt");
        }
        limiter.check(user.getId());

        List<String> earlier = in.previousHints() == null ? List.of() : in.previousHints().stream().filter(h -> h != null && !h.isBlank()).toList();
        int level = earlier.size() + 1;

        String system = """
                You are Bunny, a friendly tutor inside a learning app. A student is answering ONE quiz question and asked for hint %d.
                You know the correct answer (SECRET below) and the lesson facts. Your job is to TEACH the idea behind the question so the
                student can work the answer out themselves.

                How to write the hint:
                - State a true, concrete fact or idea from the topic (use the lesson facts) that leads to the right answer.
                - Talk about the subject itself (e.g. Python, CSS), not about the quiz: never say "look for the phrase", "the question highlights",
                  "focus on the option that…", "the only option that…", or refer to options by letter or position.
                - Never reveal, quote or paraphrase the correct option, and never name or eliminate specific options.
                - Hint 1: the key idea in general terms. Hint 2: the same idea more concretely, with a tiny example.
                  Hint 3+: the strongest teaching nudge; for "select all" questions you may say how many answers are correct.
                - If the student has already chosen an answer, you may say whether they are on the right track or that something doesn't fit,
                  and why in terms of the topic — but never confirm the full answer.
                - Never repeat an earlier hint. 1–2 short sentences, plain text, no preamble.

                Example
                Question: Which keyword creates a variable that cannot be reassigned in JavaScript?
                Bad hint: "Look for the option whose name suggests something constant." (talks about options)
                Good hint 1: "Some variables are meant to stay fixed once they get a value — JavaScript has a keyword made exactly for that."
                Good hint 2: "If you write x = 1 and later try x = 2, one of the keywords makes JavaScript throw an error instead — it's short for a math word meaning 'never changes'."
                %s""".formatted(level, languageRule(language));

        StringBuilder prompt = new StringBuilder()
                .append("Quiz: ").append(title).append("\n")
                .append("Question type: ").append(typeLabel(in.type(), in.correct().size())).append("\n")
                .append("Question: ").append(in.question()).append("\n");
        if (!in.options().isEmpty()) prompt.append("Options:\n").append(numbered(in.options())).append("\n");
        prompt.append("SECRET correct answer: ").append(secret(in)).append("\n");
        prompt.append("Student's current answer: ").append(in.currentAnswer() == null || in.currentAnswer().isBlank() ? "(nothing chosen yet)" : in.currentAnswer()).append("\n");
        if (!earlier.isEmpty()) {
            prompt.append("Hints already given (do not repeat):\n");
            earlier.forEach(h -> prompt.append("- ").append(h).append("\n"));
        }
        if (!context.isBlank()) {
            prompt.append("Lesson facts (use these to teach):\n")
                    .append(context.length() > 3000 ? context.substring(0, 3000) : context).append("\n");
        }

        List<GroqClient.Message> messages = List.of(GroqClient.Message.user(prompt.toString()));
        String hint = ai.chat(system, messages, 300, 0.3, "medium");
        // Safety net: a hint that names a correct answer the student hasn't picked is rewritten once, then replaced
        if (leaksAnswer(hint, in)) {
            hint = ai.chat(system + "\nIMPORTANT: your previous draft named the answer. Rewrite the hint without mentioning any option or answer by name.",
                    messages, 300, 0.2, "medium");
            if (leaksAnswer(hint, in)) hint = fallbackHint(in, language);
        }

        AiHintUsage row = usage.findByUserIdAndScopeAndRefIdAndAttemptNo(user.getId(), scope, refId, attemptNo)
                .orElseGet(() -> new AiHintUsage(user.getId(), scope, refId, attemptNo));
        row.increment();
        usage.save(row);
        return hint;
    }

    private String explain(String title, String context, QuestionType type, String question, List<String> options, List<String> correct,
                           List<String> accepted, String studentAnswer, String teacherNote, String language) {
        String system = """
                You explain a quiz answer to a student who has already submitted.
                In at most 100 words, plain text, no preamble:
                - Say why the correct answer is correct, about this exact question.
                - If the student's answer differs: name each wrong choice and why it is wrong, and any correct choice they missed.
                - If the student was right, confirm it briefly and add one sentence that deepens understanding.
                Be clear and encouraging. Do not tell them to re-read the lesson.
                %s""".formatted(languageRule(language));
        String prompt = "Quiz: " + title + "\n"
                + "Question type: " + typeLabel(type, correct.size()) + "\n"
                + (context.isBlank() ? "" : "Background from the lesson:\n" + (context.length() > 2000 ? context.substring(0, 2000) : context) + "\n")
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

    /** True when the hint mentions a correct option (or accepted answer) that the student hasn't already chosen. */
    private static boolean leaksAnswer(String hint, HintInput in) {
        String h = hint.toLowerCase(java.util.Locale.ROOT);
        String chosen = in.currentAnswer() == null ? "" : in.currentAnswer().toLowerCase(java.util.Locale.ROOT);
        List<String> secrets = in.type() == QuestionType.SHORT_ANSWER
                ? (in.accepted() == null ? List.of() : in.accepted())
                : in.type() == QuestionType.TRUE_FALSE ? List.of() : in.correct();
        return secrets.stream()
                .map(x -> x == null ? "" : x.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(x -> x.length() >= 3 && !chosen.contains(x))
                .anyMatch(h::contains);
    }

    private static String fallbackHint(HintInput in, String language) {
        boolean km = "km".equals(language);
        if (in.type() == QuestionType.MULTIPLE_CHOICE) {
            return km ? "សំណួរនេះមានចម្លើយត្រឹមត្រូវ " + in.correct().size() + "។ ពិនិត្យជម្រើសនីមួយៗ ជាមួយគំនិតសំខាន់ក្នុងសំណួរ។"
                    : "This question has " + in.correct().size() + " correct answers. Check each option against the key idea in the question.";
        }
        return km ? "រកពាក្យសំខាន់ក្នុងសំណួរ ហើយពិនិត្យជម្រើសនីមួយៗថាត្រូវនឹងគំនិតនោះឬអត់។"
                : "Find the key word in the question and check each option against that idea.";
    }

    private static String typeLabel(QuestionType type, int correctCount) {
        if (type == null) return "single choice";
        return switch (type) {
            case MULTIPLE_CHOICE -> "select all that apply (" + correctCount + " correct options)";
            case TRUE_FALSE -> "true or false";
            case SHORT_ANSWER -> "short typed answer";
            default -> "single choice (exactly one correct option)";
        };
    }

    private static String secret(HintInput in) {
        if (in.type() == QuestionType.SHORT_ANSWER) {
            return "any of: " + String.join(" / ", in.accepted() == null ? List.of() : in.accepted());
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < in.options().size(); i++) {
            if (in.correct().contains(in.options().get(i))) parts.add((char) ('A' + i) + ". " + in.options().get(i));
        }
        return String.join("; ", parts);
    }

    private static String picked(List<String> options, List<Integer> selected) {
        return (selected == null ? List.<Integer>of() : selected).stream()
                .filter(i -> i != null && i >= 0 && i < options.size())
                .map(i -> (char) ('A' + i) + ". " + options.get(i))
                .collect(Collectors.joining(", "));
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
