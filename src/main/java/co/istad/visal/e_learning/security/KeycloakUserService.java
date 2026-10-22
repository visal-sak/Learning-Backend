package co.istad.visal.e_learning.security;

import co.istad.visal.e_learning.expection.NotFoundException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
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
public class KeycloakUserService {

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
     * Swaps whichever application role the user currently holds for a new one.
     * Users get exactly one of STUDENT / INSTRUCTOR / ADMIN.
     */
    public void replaceRole(String keycloakSub, KeycloakRoleEnum role) {
        UserResource user = userResource(keycloakSub);

        List<RoleRepresentation> existing = user.roles().realmLevel().listEffective().stream()
                .filter(r -> isApplicationRole(r.getName()))
                .toList();

        if (!existing.isEmpty()) {
            user.roles().realmLevel().remove(existing);
        }

        user.roles().realmLevel().add(List.of(
                realmResource().roles().get(role.name()).toRepresentation()));
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