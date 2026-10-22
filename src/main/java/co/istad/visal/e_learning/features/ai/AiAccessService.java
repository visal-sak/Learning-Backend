package co.istad.visal.e_learning.features.ai;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.ai.dto.AiAccessDtos.*;
import co.istad.visal.e_learning.features.content.Lesson;
import co.istad.visal.e_learning.features.content.LessonRepository;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.enrollment.EnrollmentRepository;
import co.istad.visal.e_learning.features.notification.MailService;
import co.istad.visal.e_learning.features.notification.NotificationService;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.features.user.UserRepository;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admins (any course) and instructors (their own courses) give students extra Bunny AI help on a lesson.
 * The student gets an in-app notification right away and, if asked, an email.
 */
@Service
@Transactional(readOnly = true)
public class AiAccessService {

    private final AiAccessGrantRepository grants;
    private final LessonRepository lessons;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final CourseAccessService access;
    private final CurrentUserService currentUser;
    private final NotificationService notifications;
    private final MailService mail;

    public AiAccessService(AiAccessGrantRepository grants, LessonRepository lessons, CourseRepository courses,
                           EnrollmentRepository enrollments, UserRepository users, CourseAccessService access,
                           CurrentUserService currentUser, NotificationService notifications, MailService mail) {
        this.grants = grants;
        this.lessons = lessons;
        this.courses = courses;
        this.enrollments = enrollments;
        this.users = users;
        this.access = access;
        this.currentUser = currentUser;
        this.notifications = notifications;
        this.mail = mail;
    }

    /** Extra tutor questions for this student on this lesson (all grants added up). */
    public int extraQuestions(UUID userId, UUID lessonId) {
        return grants.extraQuestions(userId, lessonId);
    }

    /** Extra quiz hints per attempt for this student on this lesson. */
    public int extraHints(UUID userId, UUID lessonId) {
        return grants.extraHints(userId, lessonId);
    }

    public List<AiAccessGrantResponse> forCourse(UUID courseId) {
        Course course = courses.findById(courseId).orElseThrow(() -> new NotFoundException("No course with id " + courseId));
        access.requireCanEdit(course);
        List<AiAccessGrant> rows = grants.findByCourseIdOrderByCreatedAtDesc(courseId);
        Map<UUID, User> people = users.findAllById(rows.stream().map(AiAccessGrant::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, String> lessonTitles = new java.util.HashMap<>();
        course.getSections().forEach(s -> s.getLessons().forEach(l -> lessonTitles.put(l.getId(), l.getTitle())));
        boolean showEmail = isAdmin();
        return rows.stream().map(g -> toResponse(g, people.get(g.getUserId()), lessonTitles.get(g.getLessonId()), showEmail, false)).toList();
    }

    @Transactional
    public List<AiAccessGrantResponse> grant(GrantAiAccessRequest request) {
        if (request.extraQuestions() == 0 && request.extraHints() == 0) {
            throw new ConflictException("Give at least one extra question or hint");
        }
        Lesson lesson = lessons.findWithCourse(request.lessonId())
                .orElseThrow(() -> new NotFoundException("No lesson with id " + request.lessonId()));
        Course course = lesson.getSection().getCourse();
        access.requireCanEdit(course);
        User me = currentUser.require();
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        String link = "/courses/" + course.getId() + "/learn/" + lesson.getId();

        List<AiAccessGrantResponse> created = new ArrayList<>();
        for (UUID userId : request.userIds().stream().distinct().toList()) {
            User student = users.findById(userId).orElseThrow(() -> new NotFoundException("No user with id " + userId));
            if (!enrollments.existsByUserIdAndCourseId(userId, course.getId())) {
                throw new ConflictException("That student is not enrolled in this course");
            }
            AiAccessGrant saved = grants.save(new AiAccessGrant(userId, course.getId(), lesson.getId(),
                    request.extraQuestions(), request.extraHints(), note, me.getId(), me.getDisplayName()));
            boolean emailQueued = request.sendEmail() && mail.isConfigured() && student.getEmail() != null && !student.getEmail().isBlank();
            created.add(toResponse(saved, student, lesson.getTitle(), isAdmin(), emailQueued));

            String title = "Extra Bunny AI help for “" + lesson.getTitle() + "”";
            String body = describe(request.extraQuestions(), request.extraHints())
                    + " in " + course.getTitle() + "." + (note == null ? "" : "\n“" + note + "” — " + me.getDisplayName());
            notifications.notify(userId, "AI_ACCESS_GRANTED", title, body, link);
            if (request.sendEmail()) {
                mail.send(student.getEmail(), "🐰 " + title, "You've got extra Bunny AI help", body, "Open the lesson", link);
            }
        }
        return created;
    }

    @Transactional
    public void revoke(UUID id) {
        AiAccessGrant g = grants.findById(id).orElseThrow(() -> new NotFoundException("No grant with id " + id));
        Course course = courses.findById(g.getCourseId()).orElseThrow(() -> new NotFoundException("No course with id " + g.getCourseId()));
        access.requireCanEdit(course);
        grants.delete(g);
    }

    private static String describe(int questions, int hints) {
        List<String> parts = new ArrayList<>();
        if (questions > 0) parts.add(questions + " more Bunny AI tutor question" + (questions == 1 ? "" : "s"));
        if (hints > 0) parts.add(hints + " more quiz hint" + (hints == 1 ? "" : "s") + " per attempt");
        return "You can now ask " + String.join(" and ", parts);
    }

    private boolean isAdmin() {
        return currentUser.hasRole(co.istad.visal.e_learning.security.KeycloakRoleEnum.ADMIN);
    }

    private static AiAccessGrantResponse toResponse(AiAccessGrant g, User student, String lessonTitle, boolean showEmail, boolean emailQueued) {
        return new AiAccessGrantResponse(g.getId(), g.getUserId(),
                student == null ? null : student.getDisplayName(), student == null || !showEmail ? null : student.getEmail(),
                g.getCourseId(), g.getLessonId(), lessonTitle, g.getExtraQuestions(), g.getExtraHints(), g.getNote(),
                g.getGrantedByName(), g.getCreatedAt(), emailQueued);
    }
}
