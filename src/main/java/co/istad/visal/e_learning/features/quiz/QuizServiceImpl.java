package co.istad.visal.e_learning.features.quiz;

import co.istad.visal.e_learning.features.ai.AiHintUsageRepository;
import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.features.certificate.CertificateService;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.enrollment.Enrollment;
import co.istad.visal.e_learning.features.quiz.dto.instructor.*;
import co.istad.visal.e_learning.features.quiz.dto.student.AttemptResultResponse;
import co.istad.visal.e_learning.features.quiz.dto.student.QuizTakeResponse;
import co.istad.visal.e_learning.features.quiz.dto.student.SubmitAttemptRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class QuizServiceImpl implements QuizService {

    private final AiHintUsageRepository hintUsage;

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final CourseRepository courseRepository;
    private final QuizMapper mapper;
    private final CourseAccessService access;
    private final CertificateService certificates;

    public QuizServiceImpl(QuizRepository quizRepository,
                           QuestionRepository questionRepository,
                           QuizAttemptRepository attemptRepository,
                           AttemptAnswerRepository attemptAnswerRepository,
                           CourseRepository courseRepository,
                           QuizMapper mapper,
                           CourseAccessService access,
                           CertificateService certificates,
                           AiHintUsageRepository hintUsage) {
        this.hintUsage = hintUsage;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.courseRepository = courseRepository;
        this.mapper = mapper;
        this.access = access;
        this.certificates = certificates;
    }

    // ---------------------------------------------------------------
    //  Authoring
    // ---------------------------------------------------------------

    @Override
    public QuizDetailResponse getForEditingByCourse(UUID courseId) {
        Course course = loadCourse(courseId);
        access.requireCanEdit(course);
        Quiz quiz = quizForCourse(courseId)
                .orElseThrow(() -> new NotFoundException("This course has no quiz"));
        return mapper.toDetail(quiz);
    }

    @Override
    public QuizDetailResponse getForEditing(UUID quizId) {
        Quiz quiz = load(quizId);
        access.requireCanEdit(quiz.getCourse());
        return mapper.toDetail(quiz);
    }

    @Override
    @Transactional
    public QuizDetailResponse create(CreateQuizRequest request) {
        Course course = loadCourse(request.courseId());
        access.requireCanEdit(course);

        if (quizRepository.existsByCourseId(course.getId())) {
            throw new ConflictException("This course already has a quiz");
        }

        Quiz quiz = new Quiz(course, request.title().trim());
        quiz.setInstructions(request.instructions());
        quiz.setPassMark(request.passMark());
        quiz.setMaxAttempts(request.maxAttempts());
        quiz.setTimeLimitMinutes(request.timeLimitMinutes());

        return mapper.toDetail(quizRepository.save(quiz));
    }

    @Override
    @Transactional
    public QuizDetailResponse update(UUID quizId, UpdateQuizRequest request) {
        Quiz quiz = load(quizId);
        access.requireCanEdit(quiz.getCourse());

        quiz.setTitle(request.title().trim());
        quiz.setInstructions(request.instructions());
        quiz.setPassMark(request.passMark());
        quiz.setMaxAttempts(request.maxAttempts());
        quiz.setTimeLimitMinutes(request.timeLimitMinutes());

        return mapper.toDetail(quiz);
    }

    @Override
    @Transactional
    public void delete(UUID quizId) {
        Quiz quiz = load(quizId);
        access.requireCanEdit(quiz.getCourse());

        if (attemptRepository.findByQuizIdOrderBySubmittedAtDesc(quizId).size() > 0) {
            throw new ConflictException("Students have already attempted this quiz; edit it instead of deleting");
        }

        quizRepository.delete(quiz);
    }

    @Override
    @Transactional
    public QuestionResponse saveQuestion(SaveQuestionRequest request) {
        Quiz quiz = load(request.quizId());
        access.requireCanEdit(quiz.getCourse());
        QuestionType type = typeOf(request);
        validateAnswers(type, request);

        int position = (int) questionRepository.countByQuizId(quiz.getId());
        Question question = new Question(quiz, request.text().trim(), position);
        question.setExplanation(request.explanation());
        question.setPoints(request.points());
        question.setType(type);

        applyAnswers(question, type, request);

        return mapper.toResponse(questionRepository.save(question));
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(UUID questionId, SaveQuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("No question with id " + questionId));

        access.requireCanEdit(question.getQuiz().getCourse());
        QuestionType type = typeOf(request);
        validateAnswers(type, request);

        question.setText(request.text().trim());
        question.setExplanation(request.explanation());
        question.setPoints(request.points());
        question.setType(type);

        question.getOptions().clear();
        applyAnswers(question, type, request);

        return mapper.toResponse(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("No question with id " + questionId));

        access.requireCanEdit(question.getQuiz().getCourse());
        questionRepository.delete(question);
    }

    @Override
    public List<AttemptResultResponse> findAllAttempts(UUID quizId) {
        Quiz quiz = load(quizId);
        access.requireCanViewStudentData(quiz.getCourse());

        return attemptRepository.findByQuizIdOrderBySubmittedAtDesc(quizId).stream()
                .map(this::toResult)
                .toList();
    }

    // ---------------------------------------------------------------
    //  Taking
    // ---------------------------------------------------------------

    /**
     * Builds the student view. Note that it maps through toStudentView, whose
     * target record has no isCorrect component at all — there is no code path
     * from here that could leak the answer key.
     */
    @Override
    public QuizTakeResponse startAttempt(UUID courseId) {
        Course course = loadCourse(courseId);
        Enrollment enrollment = access.requireEnrollment(course);

        Quiz quiz = quizRepository.findByCourseId(courseId)
                .orElseThrow(() -> new NotFoundException("This course has no quiz"));

        long used = attemptRepository.countByEnrollmentIdAndQuizId(enrollment.getId(), quiz.getId());

        if (quiz.getMaxAttempts() != null && used >= quiz.getMaxAttempts()) {
            throw new ConflictException("You have used all " + quiz.getMaxAttempts() + " attempts");
        }

        return new QuizTakeResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getInstructions(),
                quiz.getPassMark(),
                quiz.getTimeLimitMinutes(),
                (int) used + 1,
                quiz.getMaxAttempts(),
                quiz.getQuestions().stream().map(mapper::toStudentView).toList());
    }

    /**
     * Scoring happens here and only here. The request carries option ids; the
     * answer key is read from the database on this side of the wire.
     */
    @Override
    @Transactional
    public AttemptResultResponse submit(UUID quizId, SubmitAttemptRequest request) {
        Quiz quiz = load(quizId);
        Enrollment enrollment = access.requireEnrollment(quiz.getCourse());

        long used = attemptRepository.countByEnrollmentIdAndQuizId(enrollment.getId(), quizId);
        if (quiz.getMaxAttempts() != null && used >= quiz.getMaxAttempts()) {
            throw new ConflictException("You have used all " + quiz.getMaxAttempts() + " attempts");
        }

        Map<UUID, SubmitAttemptRequest.Answer> submitted = request.answers().stream()
                .collect(Collectors.toMap(
                        SubmitAttemptRequest.Answer::questionId,
                        Function.identity(),
                        (first, second) -> first));

        QuizAttempt attempt = new QuizAttempt(enrollment, quiz, (short) (used + 1));

        int earned = 0;
        int possible = 0;
        List<AttemptAnswer> answers = new ArrayList<>();

        for (Question question : quiz.getQuestions()) {
            possible += question.getPoints();

            AttemptAnswer answer = grade(attempt, question, submitted.get(question.getId()));
            if (answer.isCorrect()) {
                earned += question.getPoints();
            }

            answers.add(answer);
        }

        // Hints used on this attempt (AI helper in the final quiz) can cost points
        int hints = hintUsage.used(enrollment.getUser().getId(), AiHintUsageRepository.FINAL_QUIZ, quizId, (int) used + 1);
        int questionCount = quiz.getQuestions().size();
        double deduction = questionCount == 0 ? 0 : hints * quiz.getCourse().getAiHintPenalty() * possible / (double) questionCount;
        short score = (short) (possible == 0 ? 0 : Math.round(Math.max(0, earned - deduction) * 100.0 / possible));
        attempt.grade(score, score >= quiz.getPassMark());
        attempt.getAnswers().addAll(answers);

        QuizAttempt saved = attemptRepository.saveAndFlush(attempt);
        // Passing the quiz may be the last requirement for the certificate
        if (saved.isPassed()) certificates.issueIfEligible(enrollment);
        return toResult(saved);
    }

    @Override
    public List<AttemptResultResponse> findMyAttempts(UUID quizId) {
        Quiz quiz = load(quizId);
        Enrollment enrollment = access.requireEnrollment(quiz.getCourse());

        return attemptRepository
                .findByEnrollmentIdAndQuizIdOrderByAttemptNoDesc(enrollment.getId(), quizId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    // ---------------------------------------------------------------

    /**
     * Correct answers appear in this DTO, which is fine: it is only ever built
     * from a submitted attempt.
     */
    private AttemptResultResponse toResult(QuizAttempt attempt) {
        List<AttemptAnswer> answers = attempt.getAnswers().isEmpty()
                ? attemptAnswerRepository.findByAttemptId(attempt.getId())
                : attempt.getAnswers();

        Map<UUID, AttemptAnswer> byQuestion = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity(), (a, b) -> a));

        List<AttemptResultResponse.QuestionResultResponse> details =
                attempt.getQuiz().getQuestions().stream()
                        .map(question -> {
                            AttemptAnswer given = byQuestion.get(question.getId());
                            return toQuestionResult(question, given);
                        })
                        .toList();

        int correctCount = (int) details.stream()
                .filter(AttemptResultResponse.QuestionResultResponse::correct)
                .count();

        return new AttemptResultResponse(
                attempt.getId(),
                attempt.getScore(),
                attempt.isPassed(),
                attempt.getQuiz().getPassMark(),
                correctCount,
                details.size(),
                attempt.getAttemptNo(),
                attempt.getSubmittedAt(),
                toStudent(attempt),
                details);
    }

    private static AttemptResultResponse.Student toStudent(QuizAttempt attempt) {
        if (attempt.getEnrollment() == null || attempt.getEnrollment().getUser() == null) return null;
        var user = attempt.getEnrollment().getUser();
        return new AttemptResultResponse.Student(user.getId(), user.getDisplayName(), user.getEmail(), user.getAvatarUrl());
    }

    // ---------------------------------------------------------------
    //  Question types
    // ---------------------------------------------------------------

    private static QuestionType typeOf(SaveQuestionRequest request) {
        return request.type() == null ? QuestionType.SINGLE_CHOICE : request.type();
    }

    /** Each type has its own answer-key rules; reject anything that could not be graded. */
    private void validateAnswers(QuestionType type, SaveQuestionRequest request) {
        List<SaveQuestionRequest.OptionInput> options = request.options() == null ? List.of() : request.options();
        long correct = options.stream().filter(SaveQuestionRequest.OptionInput::correct).count();

        switch (type) {
            case SINGLE_CHOICE -> {
                if (options.size() < 2) throw new ConflictException("Provide between two and six options");
                if (correct != 1) throw new ConflictException("Mark exactly one option as correct");
            }
            case MULTIPLE_CHOICE -> {
                if (options.size() < 2) throw new ConflictException("Provide between two and six options");
                if (correct < 2) throw new ConflictException("Mark at least two options as correct");
            }
            case TRUE_FALSE -> {
                if (request.correctAnswer() == null) throw new ConflictException("Choose whether True or False is correct");
            }
            case SHORT_ANSWER -> {
                if (acceptedLines(request.answerKey()).isEmpty()) {
                    throw new ConflictException("Add at least one accepted answer");
                }
            }
        }
    }

    /** Options (or the answer key) for the question, by type. */
    private void applyAnswers(Question question, QuestionType type, SaveQuestionRequest request) {
        question.setAnswerKey(null);

        switch (type) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> {
                List<SaveQuestionRequest.OptionInput> inputs = request.options();
                for (int i = 0; i < inputs.size(); i++) {
                    SaveQuestionRequest.OptionInput input = inputs.get(i);
                    question.getOptions().add(new AnswerOption(question, input.text().trim(), input.correct(), i));
                }
            }
            case TRUE_FALSE -> {
                boolean isTrue = request.correctAnswer();
                question.getOptions().add(new AnswerOption(question, "True", isTrue, 0));
                question.getOptions().add(new AnswerOption(question, "False", !isTrue, 1));
            }
            case SHORT_ANSWER -> question.setAnswerKey(String.join("\n", acceptedLines(request.answerKey())));
        }
    }

    /** Grades one question. A missing answer is simply incorrect. */
    private AttemptAnswer grade(QuizAttempt attempt, Question question, SubmitAttemptRequest.Answer answer) {
        switch (question.getType()) {
            case MULTIPLE_CHOICE -> {
                Set<UUID> picked = answer == null || answer.selectedOptionIds() == null
                        ? Set.of()
                        : new LinkedHashSet<>(answer.selectedOptionIds());
                Set<AnswerOption> selected = new LinkedHashSet<>();
                question.getOptions().stream().filter(o -> picked.contains(o.getId())).forEach(selected::add);

                Set<AnswerOption> correctSet = new LinkedHashSet<>(question.correctOptions());
                boolean correct = !selected.isEmpty() && selected.equals(correctSet);
                return new AttemptAnswer(attempt, question, selected, correct);
            }
            case SHORT_ANSWER -> {
                String typed = answer == null || answer.textAnswer() == null ? null : answer.textAnswer().trim();
                boolean blank = typed == null || typed.isEmpty();
                boolean correct = !blank && question.acceptedAnswers().contains(Question.normalize(typed));
                return new AttemptAnswer(attempt, question, blank ? null : typed, correct);
            }
            default -> { // SINGLE_CHOICE, TRUE_FALSE
                UUID selectedId = answer == null ? null : answer.selectedOptionId();
                AnswerOption selected = question.getOptions().stream()
                        .filter(o -> o.getId().equals(selectedId))
                        .findFirst()
                        .orElse(null);
                return new AttemptAnswer(attempt, question, selected, selected != null && selected.isCorrect());
            }
        }
    }

    private AttemptResultResponse.QuestionResultResponse toQuestionResult(Question question, AttemptAnswer given) {
        QuestionType type = question.getType();
        boolean correct = given != null && given.isCorrect();

        if (type == QuestionType.SHORT_ANSWER) {
            List<String> accepted = acceptedLines(question.getAnswerKey());
            String typed = given == null ? null : given.getTextAnswer();
            return new AttemptResultResponse.QuestionResultResponse(
                    question.getId(), question.getText(), type,
                    null, typed,
                    null, String.join(" / ", accepted),
                    List.of(), List.of(),
                    typed, accepted,
                    correct, question.getExplanation());
        }

        // Options picked: the whole set for MULTIPLE_CHOICE, the single option otherwise
        List<AnswerOption> picked;
        if (given == null) {
            picked = List.of();
        } else if (type == QuestionType.MULTIPLE_CHOICE) {
            picked = question.getOptions().stream().filter(given.getSelectedOptions()::contains).toList();
        } else {
            picked = given.getSelectedOption() == null ? List.of() : List.of(given.getSelectedOption());
        }
        List<AnswerOption> right = question.correctOptions();

        return new AttemptResultResponse.QuestionResultResponse(
                question.getId(), question.getText(), type,
                picked.isEmpty() ? null : picked.get(0).getId(),
                picked.isEmpty() ? null : joinTexts(picked),
                right.isEmpty() ? null : right.get(0).getId(),
                right.isEmpty() ? null : joinTexts(right),
                picked.stream().map(AnswerOption::getId).toList(),
                right.stream().map(AnswerOption::getId).toList(),
                null, List.of(),
                correct, question.getExplanation());
    }

    private static String joinTexts(List<AnswerOption> options) {
        return options.stream().map(AnswerOption::getText).collect(Collectors.joining(", "));
    }

    /** Accepted answers as the instructor typed them (trimmed, blank lines dropped). */
    private static List<String> acceptedLines(String answerKey) {
        if (answerKey == null) return List.of();
        return Arrays.stream(answerKey.split("\\R")).map(String::trim).filter(l -> !l.isEmpty()).toList();
    }

    private Quiz load(UUID id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No quiz with id " + id));
    }

    private Course loadCourse(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No course with id " + id));
    }

    private Optional<Quiz> quizForCourse(UUID courseId) {
        return quizRepository.findByCourseId(courseId);
    }
}
