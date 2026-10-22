package co.istad.visal.e_learning.features.ai;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * How many AI hints a student used on one attempt of a quiz.
 * scope = LESSON_CHECK (refId = lesson id) or FINAL_QUIZ (refId = quiz id);
 * attemptNo = the attempt the hints belong to (the next one to be submitted).
 */
@Entity
@Table(name = "ai_hint_usage", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ai_hint_usage", columnNames = {"user_id", "scope", "ref_id", "attempt_no"})
})
public class AiHintUsage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String scope;

    @Column(name = "ref_id", nullable = false)
    private UUID refId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(nullable = false)
    private int used;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiHintUsage() {
    }

    public AiHintUsage(UUID userId, String scope, UUID refId, int attemptNo) {
        this.userId = userId;
        this.scope = scope;
        this.refId = refId;
        this.attemptNo = attemptNo;
    }

    public void increment() {
        used++;
        updatedAt = Instant.now();
    }

    public int getUsed() { return used; }
}
