package co.istad.visal.e_learning.features.user;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Local mirror of a Keycloak account. Identity fields are copies; profile
 * fields are ours. There is deliberately no password column — credentials
 * are Keycloak's responsibility and must never be stored here.
 *
 * This absorbed the old user_profiles table. Two tables keyed on the same
 * person, written by two different code paths, drift the moment one path is
 * skipped — and a user created in the Keycloak console skips registration
 * entirely, so they would have had a users row and no profile at all.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_keycloak_sub", columnNames = "keycloak_sub"),
        @UniqueConstraint(name = "uq_users_email", columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * The sub claim. Not the primary key: a UUID of our own means the rest of
     * the schema keeps one id type throughout, and a Keycloak migration would
     * not rewrite every foreign key in the database.
     */
    @Column(name = "keycloak_sub", nullable = false, length = 64, updatable = false)
    private String keycloakSub;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(length = 160)
    private String headline;

    /** Was UserProfile.biography. */
    @Column(columnDefinition = "text")
    private String bio;

    /** Was UserProfile.pictureProfile. */
    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(length = 20)
    private String gender;

    @Column(name = "facebook_profile", length = 255)
    private String facebookProfile;

    @Column(name = "telegram_profile", length = 255)
    private String telegramProfile;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String keycloakSub, String email, String displayName) {
        this.keycloakSub = keycloakSub;
        this.email = email;
        this.displayName = displayName;
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
    public String getKeycloakSub() { return keycloakSub; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getFacebookProfile() { return facebookProfile; }
    public void setFacebookProfile(String facebookProfile) { this.facebookProfile = facebookProfile; }
    public String getTelegramProfile() { return telegramProfile; }
    public void setTelegramProfile(String telegramProfile) { this.telegramProfile = telegramProfile; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}