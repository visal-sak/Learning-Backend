package co.istad.visal.e_learning.features.content;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    /** Blog-style page: headings, callouts, code examples, images (JSON list of LessonBlock). */
    @Column(columnDefinition = "text")
    private String blocks;

    /**
     * A storage key, not a public URL. The service signs it per request after
     * the access check; a permanent link makes the check decorative.
     */
    @Column(name = "video_url", columnDefinition = "text")
    private String videoUrl;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    /** The public/gated boundary. Preview lessons skip the enrollment check. */
    @Column(name = "is_preview", nullable = false)
    private boolean preview;

    @Column(nullable = false)
    private int position;

    /** LESSON (video / reading) or QUIZ (a quiz item placed in the curriculum). Null on old rows = LESSON. */
    @Column(length = 10)
    private String kind;

    /** Questions in this item's quiz, kept here so the public outline can show "5 questions". */
    @Column(name = "question_count")
    private Integer questionCount;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonFile> files = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Lesson() {
    }

    public Lesson(Section section, String title, int position) {
        this.section = section;
        this.title = title;
        this.position = position;
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

    public UUID getId() { return id; }
    public Section getSection() { return section; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getBlocks() { return blocks; }
    public void setBlocks(String blocks) { this.blocks = blocks; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public boolean isPreview() { return preview; }
    public void setPreview(boolean preview) { this.preview = preview; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public List<LessonFile> getFiles() { return files; }
    public String getKind() { return kind == null ? "LESSON" : kind; }
    public void setKind(String kind) { this.kind = kind == null ? "LESSON" : kind; }
    public boolean isQuiz() { return "QUIZ".equals(kind); }
    public int getQuestionCount() { return questionCount == null ? 0 : questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
}
