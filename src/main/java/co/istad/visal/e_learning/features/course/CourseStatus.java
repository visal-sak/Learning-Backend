package co.istad.visal.e_learning.features.course;

import co.istad.visal.e_learning.security.KeycloakRoleEnum;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The state machine. Validating the TRANSITION rather than the target is the
 * whole point: without it an instructor can PATCH a rejected course straight
 * to PUBLISHED and skip review entirely.
 *
 * Taking the enum rather than a String matters here. canMoveTo(target, "Admin")
 * would compile fine and silently fall through to the instructor rules.
 */
public enum CourseStatus {

    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    REJECTED,
    ARCHIVED;

    private static final Map<CourseStatus, Set<CourseStatus>> INSTRUCTOR_MOVES = Map.of(
            DRAFT,          Set.of(PENDING_REVIEW),
            PUBLISHED,      Set.of(ARCHIVED),
            REJECTED,       Set.of(DRAFT),
            PENDING_REVIEW, Set.of(),
            ARCHIVED,       Set.of()
    );

    private static final Map<CourseStatus, Set<CourseStatus>> ADMIN_MOVES = Map.of(
            PENDING_REVIEW, Set.of(PUBLISHED, REJECTED),
            DRAFT,          Set.of(PUBLISHED),
            PUBLISHED,      Set.of(ARCHIVED),
            REJECTED,       Set.of(DRAFT),
            ARCHIVED,       Set.of(PUBLISHED)
    );

    public boolean canMoveTo(CourseStatus target, KeycloakRoleEnum role) {
        return movesFor(role).getOrDefault(this, Set.of()).contains(target);
    }

    public List<CourseStatus> allowedFor(KeycloakRoleEnum role) {
        return List.copyOf(movesFor(role).getOrDefault(this, Set.of()));
    }

    public boolean isPubliclyVisible() {
        return this == PUBLISHED;
    }

    private static Map<CourseStatus, Set<CourseStatus>> movesFor(KeycloakRoleEnum role) {
        return role == KeycloakRoleEnum.ADMIN ? ADMIN_MOVES : INSTRUCTOR_MOVES;
    }
}