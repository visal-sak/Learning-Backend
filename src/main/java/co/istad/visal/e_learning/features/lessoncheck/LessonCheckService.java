package co.istad.visal.e_learning.features.lessoncheck;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.ai.AiHintUsageRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.enrollment.EnrollmentService;
import co.istad.visal.e_learning.features.enrollment.dto.ProgressResponse;
import co.istad.visal.e_learning.features.lessoncheck.dto.CheckQuestion;
import co.istad.visal.e_learning.features.lessoncheck.dto.LessonCheckDtos.*;
import co.istad.visal.e_learning.features.quiz.QuestionType;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.IntStream;

/**
 * "Check your understanding" questions at the end of a lesson.
 * Answer keys stay on the server: students get the questions without them,
 * submit their answers, and only then see what was right and why.
 */
@Service
@Transactional(readOnly = true)
public class LessonCheckService {

    private static final TypeReference<List<CheckQuestion>> LIST = new TypeReference<>() {};
    private static final List<String> TRUE_FALSE = List.of("True", "False");

    private final LessonCheckRepository checks;
    private final LessonCheckResultRepository results;
    private final LessonRepository lessons;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;
    private final JsonMapper json;
    private final EnrollmentService enrollments;
    private final AiHintUsageRepository hintUsage;

    public LessonCheckService(LessonCheckRepository checks, LessonCheckResultRepository results, LessonRepository lessons,
                              CourseAccessService access, CurrentUserService currentUser, JsonMapper json,
                              EnrollmentService enrollments, AiHintUsageRepository hintUsage) {
        this.enrollments = enrollments;
        this.hintUsage = hintUsage;
        this.checks = checks;
        this.results = results;
        this.lessons = lessons;
        this.access = access;
        this.currentUser = currentUser;
        this.json = json;
    }

    /** Instructor / admin view, with answer keys. */
    public LessonCheckResponse forEditing(UUID lessonId) {
        Lesson lesson = loadLesson(lessonId);
        access.requireCanEdit(lesson.getSection().getCourse());
        return new LessonCheckResponse(lessonId, questionsOf(lessonId), null, passMarkOf(lessonId), lesson.isQuiz(),
                lesson.getSection().getCourse().isRequireQuizPass());
    }

    @Transactional
    public LessonCheckResponse save(UUID lessonId, SaveLessonCheckRequest request) {
        Lesson lesson = loadLesson(lessonId);
        access.requireCanEdit(lesson.getSection().getCourse());

        List<CheckQuestion> cleaned = request.questions().stream().map(this::validate).toList();
        Optional<LessonCheck> existing = checks.findByLessonId(lessonId);
        lesson.setQuestionCount(cleaned.size());
        boolean requirePass = lesson.getSection().getCourse().isRequireQuizPass();
        if (cleaned.isEmpty()) {
            existing.ifPresent(checks::delete);
            return new LessonCheckResponse(lessonId, List.of(), null, request.passMark(), lesson.isQuiz(), requirePass);
        }
        LessonCheck check = existing.orElseGet(() -> new LessonCheck(lesson));
        check.setQuestions(json.writeValueAsString(cleaned));
        check.setPassMark(request.passMark());
        checks.save(check);
        return new LessonCheckResponse(lessonId, cleaned, null, request.passMark(), lesson.isQuiz(), requirePass);
    }

    /** Student view: questions without answers, plus their last score. */
    public LessonCheckResponse forStudent(UUID lessonId) {
        Lesson lesson = loadLesson(lessonId);
        access.requireCanViewContent(lesson);
        List<CheckQuestion> questions = questionsOf(lessonId).stream().map(LessonCheckService::withoutAnswers).toList();
        MyResult mine = currentUser.current()
                .flatMap(u -> results.findByUserIdAndLessonId(u.getId(), lessonId))
                .map(r -> new MyResult(r.getCorrect(), r.getTotal(), percent(r.getCorrect(), r.getTotal()), r.getAttempts(), r.getUpdatedAt(), r.isPassed()))
                .orElse(null);
        return new LessonCheckResponse(lessonId, questions, mine, passMarkOf(lessonId), lesson.isQuiz(),
                lesson.getSection().getCourse().isRequireQuizPass());
    }

    @Transactional
    public CheckResultResponse submit(UUID lessonId, SubmitCheckRequest request) {
        User user = currentUser.require();
        Lesson lesson = loadLesson(lessonId);
        access.requireCanViewContent(lesson);
        List<CheckQuestion> questions = questionsOf(lessonId);
        if (questions.isEmpty()) throw new NotFoundException("This lesson has no check");

        Map<String, SubmitCheckRequest.Answer> byId = new HashMap<>();
        for (SubmitCheckRequest.Answer a : request.answers()) {
            if (a != null && a.questionId() != null) byId.put(a.questionId(), a);
        }

        List<QuestionResult> graded = new ArrayList<>();
        int correctCount = 0;
        for (CheckQuestion q : questions) {
            SubmitCheckRequest.Answer a = byId.get(q.id());
            List<Integer> correctOptions = q.options() == null ? List.of()
                    : IntStream.range(0, q.options().size()).filter(i -> Boolean.TRUE.equals(q.options().get(i).correct())).boxed().toList();
            boolean ok = switch (q.type()) {
                case SHORT_ANSWER -> a != null && a.text() != null && !a.text().isBlank()
                        && q.acceptedAnswers().stream().map(LessonCheckService::normalize).anyMatch(normalize(a.text())::equals);
                default -> a != null && a.selected() != null && new HashSet<>(a.selected()).equals(new HashSet<>(correctOptions));
            };
            if (ok) correctCount++;
            graded.add(new QuestionResult(q.id(), ok, correctOptions,
                    q.type() == QuestionType.SHORT_ANSWER ? q.acceptedAnswers() : null, q.explanation()));
        }

        // Hints used on this attempt can cost points (course setting, e.g. 0.25 of a question per hint)
        Course course = lesson.getSection().getCourse();
        int attemptNo = results.findByUserIdAndLessonId(user.getId(), lessonId).map(r -> r.getAttempts() + 1).orElse(1);
        int hints = hintUsage.used(user.getId(), AiHintUsageRepository.LESSON_CHECK, lessonId, attemptNo);
        double earned = Math.max(0, correctCount - hints * course.getAiHintPenalty());
        int score = questions.isEmpty() ? 0 : (int) Math.round(earned * 100.0 / questions.size());
        Short passMark = passMarkOf(lessonId);
        boolean passed = passMark == null || score >= passMark;

        LessonCheckResult result = results.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseGet(() -> new LessonCheckResult(user, lesson));
        result.record(correctCount, questions.size(), passed);
        results.save(result);

        // A quiz item in the curriculum completes on submit — or only on a pass when the course requires it
        ProgressResponse progress = null;
        boolean requirePass = lesson.getSection().getCourse().isRequireQuizPass();
        if (lesson.isQuiz() && access.isEnrolled(lesson.getSection().getCourse()) && (passed || !requirePass)) {
            progress = enrollments.markLessonComplete(lessonId);
        }

        return new CheckResultResponse(correctCount, questions.size(), score, graded, passed, passMark, progress);
    }

    // ------------------------------------------------------------------

    private Lesson loadLesson(UUID id) {
        return lessons.findWithCourse(id).orElseThrow(() -> new NotFoundException("No lesson with id " + id));
    }

    /** Questions with answer keys — for the AI helper (callers check access first). */
    public List<CheckQuestion> questionsWithAnswers(UUID lessonId) {
        return questionsOf(lessonId);
    }

    private Short passMarkOf(UUID lessonId) {
        return checks.findByLessonId(lessonId).map(LessonCheck::getPassMark).orElse(null);
    }

    private List<CheckQuestion> questionsOf(UUID lessonId) {
        return checks.findByLessonId(lessonId)
                .map(c -> json.readValue(c.getQuestions(), LIST))
                .orElse(List.of());
    }

    /** Applies the same rules as the course quiz and fills in ids / True-False options. */
    private CheckQuestion validate(CheckQuestion q) {
        String id = q.id() == null || q.id().isBlank() ? UUID.randomUUID().toString() : q.id();
        String explanation = q.explanation() == null || q.explanation().isBlank() ? null : q.explanation().trim();
        List<CheckQuestion.Option> options = q.options() == null ? List.of() : q.options();
        long correct = options.stream().filter(o -> Boolean.TRUE.equals(o.correct())).count();

        switch (q.type()) {
            case SHORT_ANSWER -> {
                List<String> accepted = q.acceptedAnswers() == null ? List.of()
                        : q.acceptedAnswers().stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList();
                if (accepted.isEmpty()) throw new ConflictException("Add at least one accepted answer");
                return new CheckQuestion(id, q.type(), q.text().trim(), explanation, null, accepted);
            }
            case TRUE_FALSE -> {
                if (options.size() != 2 || correct != 1) throw new ConflictException("Choose whether True or False is correct");
                boolean trueIsCorrect = Boolean.TRUE.equals(options.get(0).correct());
                return new CheckQuestion(id, q.type(), q.text().trim(), explanation, List.of(
                        new CheckQuestion.Option(TRUE_FALSE.get(0), trueIsCorrect),
                        new CheckQuestion.Option(TRUE_FALSE.get(1), !trueIsCorrect)), null);
            }
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> {
                if (options.size() < 2 || options.size() > 6) throw new ConflictException("Provide between two and six options");
                if (q.type() == QuestionType.SINGLE_CHOICE && correct != 1) throw new ConflictException("Mark exactly one option as correct");
                if (q.type() == QuestionType.MULTIPLE_CHOICE && correct < 2) throw new ConflictException("Mark at least two options as correct");
                List<CheckQuestion.Option> clean = options.stream()
                        .map(o -> new CheckQuestion.Option(o.text().trim(), Boolean.TRUE.equals(o.correct()))).toList();
                return new CheckQuestion(id, q.type(), q.text().trim(), explanation, clean, null);
            }
            default -> throw new ConflictException("Choose a question type");
        }
    }

    private static CheckQuestion withoutAnswers(CheckQuestion q) {
        List<CheckQuestion.Option> options = q.options() == null ? null
                : q.options().stream().map(o -> new CheckQuestion.Option(o.text(), null)).toList();
        return new CheckQuestion(q.id(), q.type(), q.text(), null, options, null);
    }

    /** Case, accents and extra spaces don't matter for typed answers. */
    private static String normalize(String s) {
        String n = Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFKC);
        return n.replaceAll("\\s+", " ");
    }

    private static int percent(int correct, int total) {
        return total == 0 ? 0 : Math.round(correct * 100f / total);
    }
}
