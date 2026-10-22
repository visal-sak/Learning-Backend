package co.istad.visal.e_learning.features.course;

import co.istad.visal.e_learning.features.category.Category;
import co.istad.visal.e_learning.features.content.Section;
import co.istad.visal.e_learning.features.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses", uniqueConstraints = {
        @UniqueConstraint(name = "uq_courses_slug", columnNames = "slug")
})
public class Course {

    @Id
    @GeneratedValue
    private UUID id;

    /** The ownership layer. Every "can this instructor edit it" check reads this. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 220)
    private String slug;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    // Stored as varchar, not a Postgres enum type. A columnDefinition naming a
    // custom type only works if something created that type first — with
    // Hibernate owning the schema, nothing does, and CREATE TABLE fails.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseLevel level = CourseLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status = CourseStatus.DRAFT;

    /** The admin's rejection reason. Without it a rejected instructor is guessing. */
    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OrderBy("position ASC")
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    // Instructor settings. Nullable so existing rows keep working; null means the default.
    /** Quiz items must be passed (not just submitted) before the next item unlocks. Default off. */
    @Column(name = "require_quiz_pass")
    private Boolean requireQuizPass;

    /** The final quiz must be passed to earn the certificate. Default on. */
    @Column(name = "require_final_quiz")
    private Boolean requireFinalQuiz;

    /** Students can earn a certificate for this course. Default on. */
    @Column(name = "certificate_enabled")
    private Boolean certificateEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Course() {
    }

    public Course(User instructor, Category category, String title, String slug) {
        this.instructor = instructor;
        this.category = category;
        this.title = title;
        this.slug = slug;
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

    /** Applies a transition and keeps the timestamps consistent with it. */
    public void moveTo(CourseStatus target, String note) {
        this.status = target;
        this.reviewNote = note;

        switch (target) {
            case PENDING_REVIEW -> submittedAt = Instant.now();
            case PUBLISHED -> {
                if (publishedAt == null) {
                    publishedAt = Instant.now();
                }
            }
            default -> { }
        }
    }

    public int totalLessons() {
        return sections.stream().mapToInt(s -> s.getLessons().size()).sum();
    }

    public int totalMinutes() {
        return sections.stream()
                .flatMap(s -> s.getLessons().stream())
                .mapToInt(lesson -> lesson.getDurationMinutes())
                .sum();
    }

    public UUID getId() { return id; }
    public User getInstructor() { return instructor; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public CourseLevel getLevel() { return level; }
    public void setLevel(CourseLevel level) { this.level = level; }
    public CourseStatus getStatus() { return status; }
    public String getReviewNote() { return reviewNote; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public List<Section> getSections() { return sections; }
    public boolean isRequireQuizPass() { return Boolean.TRUE.equals(requireQuizPass); }
    public void setRequireQuizPass(boolean v) { this.requireQuizPass = v; }
    public boolean isRequireFinalQuiz() { return !Boolean.FALSE.equals(requireFinalQuiz); }
    public void setRequireFinalQuiz(boolean v) { this.requireFinalQuiz = v; }
    public boolean isCertificateEnabled() { return !Boolean.FALSE.equals(certificateEnabled); }
    public void setCertificateEnabled(boolean v) { this.certificateEnabled = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}