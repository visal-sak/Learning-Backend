package co.istad.visal.e_learning.features.user;

import co.istad.visal.e_learning.features.ai.AiAccessController;
import co.istad.visal.e_learning.features.ai.AiAccessService;
import co.istad.visal.e_learning.security.JwtRoleConverter;
import co.istad.visal.e_learning.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiAccessController.class)
@Import({SecurityConfig.class, JwtRoleConverter.class})
class AiAccessAdminOnlyTest {

    private static final String BODY = """
            {"lessonId": "%s", "userIds": ["%s"], "extraQuestions": 1, "extraHints": 1, "sendEmail": false}
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AiAccessService service;

    @MockitoBean
    private CorsConfigurationSource corsConfigurationSource;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void instructorCannotGrantBunnyAccess() throws Exception {
        mvc.perform(post("/api/v1/manage/ai-access").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().authorities(new SimpleGrantedAuthority("INSTRUCTOR"))))
                .andExpect(status().isForbidden());

        verify(service, never()).grant(any());
    }

    @Test
    void instructorCannotListOrRevokeGrants() throws Exception {
        var instructor = jwt().authorities(new SimpleGrantedAuthority("INSTRUCTOR"));

        mvc.perform(get("/api/v1/manage/ai-access").param("courseId", UUID.randomUUID().toString()).with(instructor))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/manage/ai-access/" + UUID.randomUUID()).with(instructor))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanStillGrant() throws Exception {
        mvc.perform(post("/api/v1/manage/ai-access").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isCreated());
    }
}
