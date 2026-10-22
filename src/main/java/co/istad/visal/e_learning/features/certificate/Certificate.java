package co.istad.visal.e_learning.features.certificate;

import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/**
 * Proof that a student finished a course. One per student per course.
 *
 * The names are snapshots taken at issue time: a certificate states what was
 * true when it was earned, so renaming the course or the student later must
 * not rewrite it. The course/user links are kept for "my certificates" but set
 * to null if those rows are ever deleted — the certificate itself stays
 * verifiable by its serial.
 */
@Entity
@Table(name = "certificates", uniqueConstraints = {
        @UniqueConstraint(name = "uq_certificates_serial", columnNames = "serial_no"),
        @UniqueConstraint(name = "uq_certificates_user_course", columnNames = {"user_id", "course_id"})
})
public class Certificate {

    @Id
    @GeneratedValue
    private UUID id;

    /** Public, unguessable identifier printed on the certificate, e.g. LN-2026-7K3F9QXA. */
    @Column(name = "serial_no", nullable = false, length = 24, updatable = false)
    private String serialNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Course course;

    @Column(name = "student_name", nullable = false, updatable = false)
    private String studentName;

    @Column(name = "course_title", nullable = false, updatable = false)
    private String courseTitle;

    @Column(name = "instructor_name", updatable = false)
    private String instructorName;

    /** Best quiz score at issue time; null when the course has no quiz. */
    @Column(name = "quiz_score", updatable = false)
    private Short quizScore;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    protected Certificate() {
    }

    public Certificate(String serialNo, User user, Course course, String studentName,
                       String courseTitle, String instructorName, Short quizScore) {
        this.serialNo = serialNo;
        this.user = user;
        this.course = course;
        this.studentName = studentName;
        this.courseTitle = courseTitle;
        this.instructorName = instructorName;
        this.quizScore = quizScore;
    }

    @PrePersist
    void onCreate() {
        issuedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getSerialNo() { return serialNo; }
    public User getUser() { return user; }
    public Course getCourse() { return course; }
    public String getStudentName() { return studentName; }
    public String getCourseTitle() { return courseTitle; }
    public String getInstructorName() { return instructorName; }
    public Short getQuizScore() { return quizScore; }
    public Instant getIssuedAt() { return issuedAt; }
}
