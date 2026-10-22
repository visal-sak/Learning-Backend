package co.istad.visal.e_learning.features.quiz;

import co.istad.visal.e_learning.features.course.Course;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quizzes", uniqueConstraints = {
        @UniqueConstraint(name = "uq_quizzes_course", columnNames = "course_id")
})
public class Quiz {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String instructions;

    @Column(name = "pass_mark", nullable = false)
    private short passMark = 70;

    @Column(name = "max_attempts")
    private Short maxAttempts;

    @Column(name = "time_limit_minutes")
    private Short timeLimitMinutes;

    @OrderBy("position ASC")
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Quiz() {
    }

    public Quiz(Course course, String title) {
        this.course = course;
        this.title = title;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public int totalPoints() {
        return questions.stream().mapToInt(Question::getPoints).sum();
    }

    public UUID getId() { return id; }
    public Course getCourse() { return course; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public short getPassMark() { return passMark; }
    public void setPassMark(short passMark) { this.passMark = passMark; }
    public Short getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Short maxAttempts) { this.maxAttempts = maxAttempts; }
    public Short getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Short timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }
    public List<Question> getQuestions() { return questions; }
}
