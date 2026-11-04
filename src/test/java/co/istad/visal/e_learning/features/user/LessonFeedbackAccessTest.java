package co.istad.visal.e_learning.features.user;

import co.istad.visal.e_learning.features.lessonfeedback.LessonFeedbackController;
import co.istad.visal.e_learning.features.lessonfeedback.LessonFeedbackService;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonFeedbackController.class)
@Import({SecurityConfig.class, JwtRoleConverter.class})
class LessonFeedbackAccessTest {

    private static final String LESSON = "/api/v1/lessons/" + UUID.randomUUID() + "/feedback/mine";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LessonFeedbackService service;

    @MockitoBean
    private CorsConfigurationSource corsConfigurationSource;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor as(String role) {
        return jwt().authorities(new SimpleGrantedAuthority(role));
    }

    @Test
    void aStudentCanSaveFeedback() throws Exception {
        mvc.perform(put(LESSON).contentType(MediaType.APPLICATION_JSON).content("{\"helpful\": false, \"comment\": \"Too fast\"}").with(as("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void aVisitorCannot() throws Exception {
        mvc.perform(put(LESSON).contentType(MediaType.APPLICATION_JSON).content("{\"helpful\": true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anAnswerIsRequired() throws Exception {
        mvc.perform(put(LESSON).contentType(MediaType.APPLICATION_JSON).content("{}").with(as("STUDENT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void studentsCannotReadTheList() throws Exception {
        mvc.perform(get("/api/v1/manage/lesson-feedback").with(as("STUDENT"))).andExpect(status().isForbidden());
    }

    @Test
    void instructorsCanReadButNotRemove() throws Exception {
        mvc.perform(get("/api/v1/manage/lesson-feedback").param("courseId", UUID.randomUUID().toString()).with(as("INSTRUCTOR")))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/manage/lesson-feedback/" + UUID.randomUUID()).with(as("INSTRUCTOR")))
                .andExpect(status().isForbidden());
    }
}
