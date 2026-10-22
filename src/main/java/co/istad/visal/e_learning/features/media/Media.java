package co.istad.visal.e_learning.features.media;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medias")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false, length = 10)
    private String extension;

    @Column(nullable = false)
    private Float size;

    /**
     * 100 rather than 32: some content types are long, and a value that does
     * not fit fails at insert time with a truncation error.
     */
    @Column(name = "media_type", nullable = false, length = 100)
    private String mediaType;

    /**
     * Soft delete. An upload starts live; marking it draft is the first half
     * of a two-step delete, so a file cannot vanish from under a course by
     * accident.
     */
    @Column(name = "is_draft", nullable = false)
    private Boolean isDraft = false;

    /** The Keycloak sub of whoever uploaded it. Useful when a file is orphaned. */
    @Column(name = "uploaded_by", length = 64)
    private String uploadedBy;

    /**
     * findAll sorts by this. Sorting by id would be meaningless — UUIDs are
     * not sequential, so "newest first" by id returns an arbitrary order.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}