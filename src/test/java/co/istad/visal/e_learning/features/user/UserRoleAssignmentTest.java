package co.istad.visal.e_learning.features.user;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.features.user.dto.AssignRoleRequest;
import co.istad.visal.e_learning.features.user.dto.UserAccountResponse;
import co.istad.visal.e_learning.features.user.dto.UserRolesResponse;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import co.istad.visal.e_learning.security.KeycloakUserService;
import co.istad.visal.e_learning.utils.PageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRoleAssignmentTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void signInAs(String sub) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static RoleRepresentation role(String name) {
        RoleRepresentation representation = new RoleRepresentation();
        representation.setName(name);
        return representation;
    }

    private static UserRepresentation account(String id, String email) {
        UserRepresentation user = new UserRepresentation();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private static UserServiceImpl service(KeycloakUserService keycloakUsers) {
        return new UserServiceImpl(null, null, null, keycloakUsers);
    }

    @Test
    void adminPromotesAnotherUser() {
        KeycloakUserService keycloakUsers = mock(KeycloakUserService.class);
        when(keycloakUsers.findByEmail("dara@example.com")).thenReturn(account("target", "dara@example.com"));
        when(keycloakUsers.rolesOf("target")).thenReturn(List.of("USER", "INSTRUCTOR"));
        signInAs("admin");

        UserRolesResponse response = service(keycloakUsers)
                .assignRole(new AssignRoleRequest("dara@example.com", KeycloakRoleEnum.INSTRUCTOR));

        verify(keycloakUsers).replaceRole("target", KeycloakRoleEnum.INSTRUCTOR);
        assertEquals(List.of("USER", "INSTRUCTOR"), response.roles());
    }

    @Test
    void listsAccountsWithRolesAndPageNumbers() {
        KeycloakUserService keycloakUsers = mock(KeycloakUserService.class);
        UserRepresentation account = account("target", "dara@example.com");
        account.setUsername("dara");
        when(keycloakUsers.countUsers("dara")).thenReturn(21L);
        when(keycloakUsers.listUsers("dara", 20, 20)).thenReturn(List.of(account));
        when(keycloakUsers.rolesOf("target")).thenReturn(List.of("USER", "STUDENT"));

        PageResponse<UserAccountResponse> page = service(keycloakUsers).listAccounts(" dara ", PageRequest.of(1, 20));

        assertEquals(2, page.totalPages());
        assertTrue(page.last());
        assertEquals("dara", page.content().getFirst().name());
        assertEquals(List.of("USER", "STUDENT"), page.content().getFirst().roles());
    }

    @Test
    void adminCannotChangeOwnRole() {
        KeycloakUserService keycloakUsers = mock(KeycloakUserService.class);
        when(keycloakUsers.findByEmail("admin@example.com")).thenReturn(account("admin", "admin@example.com"));
        signInAs("admin");

        assertThrows(ConflictException.class, () -> service(keycloakUsers)
                .assignRole(new AssignRoleRequest("admin@example.com", KeycloakRoleEnum.STUDENT)));

        verify(keycloakUsers, never()).replaceRole(any(), any());
    }

    @Test
    void replaceRoleKeepsUserAndAddsBeforeRemoving() {
        RoleScopeResource realmRoles = mock(RoleScopeResource.class);
        RoleRepresentation user = role("USER");
        RoleRepresentation student = role("STUDENT");
        RoleRepresentation instructor = role("INSTRUCTOR");
        when(realmRoles.listAll()).thenReturn(List.of(role("default-roles-elearning"), user, student));

        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource mappings = mock(RoleMappingResource.class);
        RolesResource roles = mock(RolesResource.class);
        RoleResource instructorRole = mock(RoleResource.class);
        when(keycloak.realm("elearning")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(realm.roles()).thenReturn(roles);
        when(users.get("target")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(mappings);
        when(mappings.realmLevel()).thenReturn(realmRoles);
        when(roles.get("INSTRUCTOR")).thenReturn(instructorRole);
        when(instructorRole.toRepresentation()).thenReturn(instructor);

        new KeycloakUserService(keycloak, "elearning").replaceRole("target", KeycloakRoleEnum.INSTRUCTOR);

        InOrder order = inOrder(realmRoles);
        order.verify(realmRoles).add(List.of(instructor));
        order.verify(realmRoles).remove(List.of(student));
    }
}
