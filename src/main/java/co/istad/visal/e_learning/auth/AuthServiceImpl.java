package co.istad.visal.e_learning.auth;

import co.istad.visal.e_learning.auth.dto.RegisterRequest;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Registration creates the account in Keycloak and nothing locally.
 *
 * The local users row is provisioned by CurrentUserService on the first
 * authenticated request. That covers accounts created here AND accounts an
 * admin creates directly in the Keycloak console — which a write at
 * registration time would miss entirely.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_GROUP = "ELearning";

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public void register(RegisterRequest registerRequest) {

        if (!registerRequest.password().equals(registerRequest.confirmedPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Passwords do not match. Please try again.");
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(registerRequest.username());
        user.setEmail(registerRequest.email());
        user.setFirstName(registerRequest.firstName());
        user.setLastName(registerRequest.lastName());
        user.setEmailVerified(true);
        user.setEnabled(true);

        // The password only reaches Keycloak because of setCredentials below.
        // Build the credential and forget this line and the account is created
        // with no password at all — Keycloak still answers 201, and nobody
        // finds out until the first login attempt fails.
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(registerRequest.password());
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        UsersResource usersResource = keycloak.realm(realm).users();

        String userId;

        try (Response response = usersResource.create(user)) {
            int status = response.getStatus();
            log.info("Keycloak user creation returned {}", status);

            // 401 and 403 here are about the ADMIN CLIENT, not the caller.
            // Check the client secret and its realm-management service-account
            // roles rather than the incoming request.
            if (status == HttpStatus.UNAUTHORIZED.value()) {
                log.error("Admin client could not authenticate against realm {}", realm);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Identity provider is not configured correctly");
            }

            if (status == HttpStatus.FORBIDDEN.value()) {
                log.error("Admin client lacks manage-users on realm {}", realm);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Identity provider is not configured correctly");
            }

            if (status == HttpStatus.CONFLICT.value()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Username or email already exists");
            }

            if (status != HttpStatus.CREATED.value()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Could not create the account. Please try again.");
            }

            // Exact id from the Location header. Searching by username is a
            // partial match — "dara" also finds "dara2" — and taking the first
            // result can hand you somebody else's account.
            userId = CreatedResponseUtil.getCreatedId(response);
        }

        log.info("Created Keycloak user {} with id {}", user.getUsername(), userId);
        assignRoles(userId);
        assignGroup(userId);
    }

    // ---------------------------------------------------------------

    private void assignRoles(String userId) {
        RolesResource rolesResource = keycloak.realm(realm).roles();

        try {
            List<RoleRepresentation> roles = List.of(
                    rolesResource.get(KeycloakRoleEnum.USER.name()).toRepresentation(),
                    rolesResource.get(KeycloakRoleEnum.STUDENT.name()).toRepresentation());

            keycloak.realm(realm).users().get(userId)
                    .roles().realmLevel().add(roles);

        } catch (jakarta.ws.rs.NotFoundException ex) {
            log.error("Realm {} is missing USER or STUDENT. Create them under Realm roles.", realm);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Identity provider is not configured correctly");
        }
    }

    private void assignGroup(String userId) {
        GroupsResource groupsResource = keycloak.realm(realm).groups();

        List<GroupRepresentation> groups = groupsResource.groups(DEFAULT_GROUP, 0, 1);

        if (groups.isEmpty()) {
            // Skip rather than throw: group membership is optional, and
            // getFirst() on an empty list would blow up a registration that
            // has otherwise succeeded.
            log.warn("Group {} not found in realm {} — skipping group assignment", DEFAULT_GROUP, realm);
            return;
        }

        UserResource keycloakUser = keycloak.realm(realm).users().get(userId);
        keycloakUser.joinGroup(groups.getFirst().getId());
    }
}