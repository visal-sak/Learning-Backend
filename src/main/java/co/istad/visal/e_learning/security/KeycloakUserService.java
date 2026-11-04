package co.istad.visal.e_learning.security;

import co.istad.visal.e_learning.expection.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Role assignment against Keycloak, for the admin "User roles" screen.
 *
 * Roles are written here and nowhere else. A local roles table would be a
 * second source of truth, and the two would disagree the first time someone
 * edited a user in the Keycloak console.
 */
@Service
@Slf4j
public class KeycloakUserService {

    /** Carries the USER and STUDENT realm roles. */
    private static final String DEFAULT_GROUP = "elearning";
    private static final int ROLE_MEMBER_PAGE = 100;

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakUserService(Keycloak keycloak, @Value("${keycloak.realm}") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    public List<String> rolesOf(String keycloakSub) {
        return userResource(keycloakSub)
                .roles().realmLevel().listEffective()
                .stream()
                .map(RoleRepresentation::getName)
                .filter(this::isApplicationRole)
                .toList();
    }

    public void assignRole(String keycloakSub, KeycloakRoleEnum role) {
        RoleRepresentation representation = realmResource().roles()
                .get(role.name())
                .toRepresentation();

        userResource(keycloakSub).roles().realmLevel().add(List.of(representation));
    }

    public void removeRole(String keycloakSub, KeycloakRoleEnum role) {
        RoleRepresentation representation = realmResource().roles()
                .get(role.name())
                .toRepresentation();

        userResource(keycloakSub).roles().realmLevel().remove(List.of(representation));
    }

    /**
     * Skips rather than throws when the group is missing: membership is optional,
     * and a realm without it should not stop anyone registering or logging in.
     */
    public void joinDefaultGroup(String keycloakSub) {
        realmResource().groups().groups(DEFAULT_GROUP, 0, 10).stream()
                .filter(group -> DEFAULT_GROUP.equalsIgnoreCase(group.getName()))
                .findFirst()
                .ifPresentOrElse(
                        group -> userResource(keycloakSub).joinGroup(group.getId()),
                        () -> log.warn("Group {} not found in realm {}, skipping group assignment", DEFAULT_GROUP, realm));
    }

    /** The whole representation goes back: a partial one can fail the realm's user profile validation. */
    public void markEmailVerified(String keycloakSub) {
        UserResource user = userResource(keycloakSub);
        UserRepresentation account = user.toRepresentation();

        if (!Boolean.TRUE.equals(account.isEmailVerified())) {
            account.setEmailVerified(true);
            user.update(account);
        }
    }

    /**
     * Swaps whichever application role the user currently holds for a new one.
     * Users get exactly one of STUDENT / INSTRUCTOR / ADMIN. USER is the base
     * role from registration and stays. The new role goes on first, so a
     * failure halfway leaves the user with too many roles rather than none.
     * Roles inherited from a group are not touched: only leaving the group drops those.
     */
    public void replaceRole(String keycloakSub, KeycloakRoleEnum role) {
        RoleScopeResource realmRoles = userResource(keycloakSub).roles().realmLevel();

        List<RoleRepresentation> stale = realmRoles.listAll().stream()
                .filter(r -> isApplicationRole(r.getName()))
                .filter(r -> !r.getName().equals(KeycloakRoleEnum.USER.name()))
                .filter(r -> !r.getName().equals(role.name()))
                .toList();

        realmRoles.add(List.of(realmResource().roles().get(role.name()).toRepresentation()));

        if (!stale.isEmpty()) {
            realmRoles.remove(stale);
        }
    }

    /** Accounts in the realm, whether or not they have logged in here yet. */
    public List<UserRepresentation> listUsers(String search, int first, int max) {
        return search == null
                ? realmResource().users().list(first, max)
                : realmResource().users().search(search, first, max);
    }

    public boolean accountExists(String keycloakSub) {
        try {
            realmResource().users().get(keycloakSub).toRepresentation();
            return true;
        } catch (jakarta.ws.rs.NotFoundException ex) {
            return false;
        }
    }

    public long countUsers(String search) {
        return search == null
                ? realmResource().users().count()
                : realmResource().users().count(search);
    }

    /** Keycloak has no count for role members, so this pages through them. */
    public long countUsersWithRole(KeycloakRoleEnum role) {
        long total = 0;
        int page;
        do {
            page = realmResource().roles().get(role.name())
                    .getUserMembers((int) total, ROLE_MEMBER_PAGE).size();
            total += page;
        } while (page == ROLE_MEMBER_PAGE);
        return total;
    }

    public UserRepresentation findByEmail(String email) {
        return realmResource().users().searchByEmail(email, true).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No Keycloak user with email " + email));
    }

    // ---------------------------------------------------------------

    private boolean isApplicationRole(String name) {
        for (KeycloakRoleEnum role : KeycloakRoleEnum.values()) {
            if (role.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private RealmResource realmResource() {
        return keycloak.realm(realm);
    }

    private UserResource userResource(String keycloakSub) {
        try {
            return realmResource().users().get(keycloakSub);
        } catch (jakarta.ws.rs.NotFoundException ex) {
            throw new NotFoundException("No Keycloak user with id " + keycloakSub);
        }
    }
}