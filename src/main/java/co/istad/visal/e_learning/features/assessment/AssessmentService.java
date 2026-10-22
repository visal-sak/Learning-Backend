package co.istad.visal.e_learning.features.assessment;

import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.assessment.dto.AssessmentDetailResponse;
import co.istad.visal.e_learning.features.assessment.dto.AssessmentSummaryResponse;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.quiz.AttemptAnswer;
import co.istad.visal.e_learning.features.quiz.AttemptAnswerRepository;
import co.istad.visal.e_learning.features.quiz.Question;
import co.istad.visal.e_learning.features.quiz.Quiz;
import co.istad.visal.e_learning.features.quiz.QuizAttempt;
import co.istad.visal.e_learning.features.quiz.QuizAttemptRepository;
import co.istad.visal.e_learning.features.quiz.QuizRepository;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Quiz results for teaching staff: an instructor sees their own courses, an
 * admin sees every course. Computed from the stored attempts on each request —
 * fine at this scale, and always in step with the data.
 */
@Service
@Transactional(readOnly = true)
public class AssessmentService {

    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;

    public AssessmentService(CourseRepository courseRepository,
                             QuizRepository quizRepository,
                             QuizAttemptRepository attemptRepository,
                             AttemptAnswerRepository answerRepository,
                             CourseAccessService access,
                             CurrentUserService currentUser) {
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.access = access;
        this.currentUser = currentUser;
    }

    /** Every course (in scope) that has a quiz. */
    public List<AssessmentSummaryResponse> list() {
        User user = currentUser.require();
        List<Course> courses = currentUser.hasRole(KeycloakRoleEnum.ADMIN)
                ? courseRepository.findAll()
                : courseRepository.findByInstructorIdOrderByUpdatedAtDesc(user.getId());

        List<AssessmentSummaryResponse> rows = new ArrayList<>();
        for (Course course : courses) {
            quizRepository.findByCourseId(course.getId())
                    .ifPresent(quiz -> rows.add(summary(course, quiz, attemptRepository.findByQuizIdOrderBySubmittedAtDesc(quiz.getId()))));
        }
        // Most active first
        rows.sort(Comparator.comparingInt(AssessmentSummaryResponse::attempts).reversed()
                .thenComparing(AssessmentSummaryResponse::courseTitle, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    public AssessmentDetailResponse detail(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("No course with id " + courseId));
        access.requireCanViewStudentData(course);

        Quiz quiz = quizRepository.findByCourseId(courseId)
                .orElseThrow(() -> new NotFoundException("This course has no quiz"));
        List<QuizAttempt> attempts = attemptRepository.findByQuizIdOrderBySubmittedAtDesc(quiz.getId());

        return new AssessmentDetailResponse(
                summary(course, quiz, attempts),
                distribution(attempts),
                questionStats(quiz, attempts),
                studentResults(attempts));
    }

    // ---------------------------------------------------------------

    private AssessmentSummaryResponse summary(Course course, Quiz quiz, List<QuizAttempt> attempts) {
        int count = attempts.size();
        long students = attempts.stream().map(a -> a.getEnrollment().getId()).distinct().count();
        Integer passRate = count == 0 ? null : (int) Math.round(attempts.stream().filter(QuizAttempt::isPassed).count() * 100.0 / count);
        Integer average = count == 0 ? null : (int) Math.round(attempts.stream().mapToInt(QuizAttempt::getScore).average().orElse(0));
        Instant last = attempts.stream().map(QuizAttempt::getSubmittedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);

        return new AssessmentSummaryResponse(
                course.getId(),
                course.getTitle(),
                course.getThumbnailUrl(),
                course.getInstructor() == null ? null : course.getInstructor().getDisplayName(),
                quiz.getId(),
                quiz.getTitle(),
                quiz.getPassMark(),
                quiz.getQuestions().size(),
                count,
                (int) students,
                passRate,
                average,
                last);
    }

    private List<AssessmentDetailResponse.Band> distribution(List<QuizAttempt> attempts) {
        int[] bands = new int[10];
        for (QuizAttempt a : attempts) bands[Math.min(9, Math.max(0, a.getScore()) / 10)]++;
        List<AssessmentDetailResponse.Band> out = new ArrayList<>();
        for (int i = 0; i < 10; i++) out.add(new AssessmentDetailResponse.Band(i * 10, i == 9 ? 100 : i * 10 + 9, bands[i]));
        return out;
    }

    private List<AssessmentDetailResponse.QuestionStat> questionStats(Quiz quiz, List<QuizAttempt> attempts) {
        Map<UUID, int[]> tally = new HashMap<>(); // questionId -> [answered, correct]
        for (QuizAttempt attempt : attempts) {
            for (AttemptAnswer answer : answerRepository.findByAttemptId(attempt.getId())) {
                int[] t = tally.computeIfAbsent(answer.getQuestion().getId(), k -> new int[2]);
                t[0]++;
                if (answer.isCorrect()) t[1]++;
            }
        }
        List<AssessmentDetailResponse.QuestionStat> out = new ArrayList<>();
        for (Question q : quiz.getQuestions()) {
            int[] t = tally.getOrDefault(q.getId(), new int[2]);
            out.add(new AssessmentDetailResponse.QuestionStat(
                    q.getId(), q.getText(), q.getType(), t[0], t[1],
                    t[0] == 0 ? null : (int) Math.round(t[1] * 100.0 / t[0])));
        }
        // Hardest first; unanswered questions last
        out.sort(Comparator.comparing(AssessmentDetailResponse.QuestionStat::correctRate, Comparator.nullsLast(Comparator.naturalOrder())));
        return out;
    }

    private List<AssessmentDetailResponse.StudentResult> studentResults(List<QuizAttempt> attempts) {
        Map<UUID, List<QuizAttempt>> byEnrollment = new LinkedHashMap<>();
        for (QuizAttempt a : attempts) byEnrollment.computeIfAbsent(a.getEnrollment().getId(), k -> new ArrayList<>()).add(a);

        List<AssessmentDetailResponse.StudentResult> out = new ArrayList<>();
        for (List<QuizAttempt> list : byEnrollment.values()) {
            User student = list.get(0).getEnrollment().getUser();
            Optional<QuizAttempt> best = list.stream().max(Comparator.comparingInt(QuizAttempt::getScore));
            out.add(new AssessmentDetailResponse.StudentResult(
                    student.getId(),
                    student.getDisplayName(),
                    student.getEmail(),
                    student.getAvatarUrl(),
                    list.size(),
                    best.map(QuizAttempt::getScore).orElse((short) 0),
                    list.stream().anyMatch(QuizAttempt::isPassed),
                    list.stream().map(QuizAttempt::getSubmittedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null)));
        }
        out.sort(Comparator.comparingInt(AssessmentDetailResponse.StudentResult::bestScore).reversed());
        return out;
    }
}
