package co.istad.visal.e_learning.security;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.features.user.User;
import co.istad.visal.e_learning.features.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultGroupTest {

    private final UserRepository users = mock(UserRepository.class);
    private final KeycloakUserService keycloakUsers = mock(KeycloakUserService.class);
    private final CurrentUserService currentUser = new CurrentUserService(users, keycloakUsers, mock(PlatformTransactionManager.class));

    @BeforeEach
    void signIn() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("sub-1")
                .claim("email", "dara@example.com").claim("name", "Dara").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(users.findByKeycloakSub("sub-1")).thenReturn(Optional.empty());
        when(users.save(any())).then(returnsFirstArg());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void firstLoginJoinsTheGroup() {
        currentUser.require();

        verify(keycloakUsers).joinDefaultGroup("sub-1");
        verify(keycloakUsers).markEmailVerified("sub-1");
    }

    @Test
    void reRegisteredAccountKeepsItsRow() {
        User old = new User("old-sub", "dara@example.com", "Dara");
        when(users.findByEmailIgnoreCase("dara@example.com")).thenReturn(Optional.of(old));
        when(keycloakUsers.accountExists("old-sub")).thenReturn(false);

        User user = currentUser.require();

        assertSame(old, user);
        assertEquals("sub-1", user.getKeycloakSub());
        verify(keycloakUsers).joinDefaultGroup("sub-1");
    }

    @Test
    void anEmailStillOwnedByALiveAccountIsNotTakenOver() {
        User other = new User("other-sub", "dara@example.com", "Dara");
        when(users.findByEmailIgnoreCase("dara@example.com")).thenReturn(Optional.of(other));
        when(keycloakUsers.accountExists("other-sub")).thenReturn(true);

        assertThrows(ConflictException.class, currentUser::require);

        assertEquals("other-sub", other.getKeycloakSub());
    }

    @Test
    void twoFirstRequestsAtOnceFindTheSameRow() {
        User winner = new User("sub-1", "dara@example.com", "Dara");
        when(users.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(users.findByKeycloakSub("sub-1")).thenReturn(Optional.empty(), Optional.of(winner));

        assertSame(winner, currentUser.require());
    }

    @Test
    void returningUserIsLeftAlone() {
        when(users.findByKeycloakSub("sub-1")).thenReturn(Optional.of(new User("sub-1", "dara@example.com", "Dara")));

        currentUser.require();

        verify(keycloakUsers, never()).joinDefaultGroup(any());
        verify(keycloakUsers, never()).markEmailVerified(any());
    }

    @Test
    void keycloakFailureDoesNotFailTheLogin() {
        doThrow(new RuntimeException("down")).when(keycloakUsers).joinDefaultGroup("sub-1");

        assertEquals("dara@example.com", currentUser.require().getEmail());
        verify(keycloakUsers).markEmailVerified("sub-1");
    }

    @Test
    void marksAnUnverifiedEmailVerified() {
        UserResource account = accountWithEmailVerified(false);
        ArgumentCaptor<UserRepresentation> saved = ArgumentCaptor.forClass(UserRepresentation.class);

        new KeycloakUserService(keycloakWith(account), "elearning").markEmailVerified("sub-1");

        verify(account).update(saved.capture());
        assertEquals(Boolean.TRUE, saved.getValue().isEmailVerified());
    }

    @Test
    void leavesAVerifiedEmailAlone() {
        UserResource account = accountWithEmailVerified(true);

        new KeycloakUserService(keycloakWith(account), "elearning").markEmailVerified("sub-1");

        verify(account, never()).update(any());
    }

    @Test
    void countsRoleMembersAcrossPages() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        RolesResource roles = mock(RolesResource.class);
        RoleResource instructor = mock(RoleResource.class);
        when(keycloak.realm("elearning")).thenReturn(realm);
        when(realm.roles()).thenReturn(roles);
        when(roles.get("INSTRUCTOR")).thenReturn(instructor);
        when(instructor.getUserMembers(0, 100)).thenReturn(Collections.nCopies(100, new UserRepresentation()));
        when(instructor.getUserMembers(100, 100)).thenReturn(Collections.nCopies(7, new UserRepresentation()));

        assertEquals(107, new KeycloakUserService(keycloak, "elearning").countUsersWithRole(KeycloakRoleEnum.INSTRUCTOR));
    }

    private static UserResource accountWithEmailVerified(boolean verified) {
        UserRepresentation representation = new UserRepresentation();
        representation.setEmailVerified(verified);
        UserResource account = mock(UserResource.class);
        when(account.toRepresentation()).thenReturn(representation);
        return account;
    }

    private static Keycloak keycloakWith(UserResource account) {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource accounts = mock(UsersResource.class);
        when(keycloak.realm("elearning")).thenReturn(realm);
        when(realm.users()).thenReturn(accounts);
        when(accounts.get("sub-1")).thenReturn(account);
        return keycloak;
    }

    @Test
    void joinsOnlyTheExactGroup() {
        GroupRepresentation similar = new GroupRepresentation();
        similar.setId("other");
        similar.setName("elearning-staff");
        GroupRepresentation exact = new GroupRepresentation();
        exact.setId("right");
        exact.setName("elearning");

        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        GroupsResource groups = mock(GroupsResource.class);
        UsersResource accounts = mock(UsersResource.class);
        UserResource account = mock(UserResource.class);
        when(keycloak.realm("elearning")).thenReturn(realm);
        when(realm.groups()).thenReturn(groups);
        when(realm.users()).thenReturn(accounts);
        when(accounts.get("sub-1")).thenReturn(account);
        when(groups.groups("elearning", 0, 10)).thenReturn(List.of(similar, exact));

        new KeycloakUserService(keycloak, "elearning").joinDefaultGroup("sub-1");

        verify(account).joinGroup("right");
        verify(account, never()).joinGroup("other");
    }
}
