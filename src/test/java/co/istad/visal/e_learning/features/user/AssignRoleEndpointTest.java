package co.istad.visal.e_learning.features.user;

import co.istad.visal.e_learning.features.user.dto.AssignRoleRequest;
import co.istad.visal.e_learning.features.user.dto.UserAccountResponse;
import co.istad.visal.e_learning.features.user.dto.UserRolesResponse;
import co.istad.visal.e_learning.security.JwtRoleConverter;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import co.istad.visal.e_learning.security.SecurityConfig;
import co.istad.visal.e_learning.utils.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtRoleConverter.class})
@TestPropertySource(properties = "app.keycloak.account-url=http://localhost/account")
class AssignRoleEndpointTest {

    private static final String BODY = """
            {"email": "dara@example.com", "role": "INSTRUCTOR"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService service;

    @MockitoBean
    private CorsConfigurationSource corsConfigurationSource;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static MockHttpServletRequestBuilder assign(String body) {
        return put("/api/v1/users/roles").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void adminCanAssignInstructor() throws Exception {
        when(service.assignRole(new AssignRoleRequest("dara@example.com", KeycloakRoleEnum.INSTRUCTOR)))
                .thenReturn(new UserRolesResponse("dara@example.com", List.of("USER", "INSTRUCTOR")));

        mvc.perform(assign(BODY).with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[1]").value("INSTRUCTOR"));
    }

    @Test
    void instructorIsForbidden() throws Exception {
        mvc.perform(assign(BODY).with(jwt().authorities(new SimpleGrantedAuthority("INSTRUCTOR"))))
                .andExpect(status().isForbidden());

        verify(service, never()).assignRole(any());
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(assign(BODY)).andExpect(status().isUnauthorized());

        verify(service, never()).assignRole(any());
    }

    @Test
    void adminCanReadAUserById() throws Exception {
        mvc.perform(get("/api/v1/users/" + UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void instructorCannotReadAUserById() throws Exception {
        mvc.perform(get("/api/v1/users/" + UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("INSTRUCTOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAccounts() throws Exception {
        when(service.listAccounts(any(), any())).thenReturn(new PageResponse<>(
                List.of(new UserAccountResponse("id", "dara", "dara@example.com", "Dara", true, List.of("USER"))),
                0, 20, 1, 1, true));

        mvc.perform(get("/api/v1/users").param("q", "dara")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("dara@example.com"));
    }

    @Test
    void instructorCannotListAccounts() throws Exception {
        mvc.perform(get("/api/v1/users").with(jwt().authorities(new SimpleGrantedAuthority("INSTRUCTOR"))))
                .andExpect(status().isForbidden());

        verify(service, never()).listAccounts(any(), any());
    }

    @Test
    void unknownRoleIsABadRequest() throws Exception {
        mvc.perform(assign("{\"email\": \"dara@example.com\", \"role\": \"OWNER\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isBadRequest());
    }
}
