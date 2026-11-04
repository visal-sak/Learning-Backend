package co.istad.visal.e_learning.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * @EnableMethodSecurity is what makes @PreAuthorize work. Leave it off and
 * every annotated endpoint quietly allows everyone through.
 *
 * Three things to know about the matchers below.
 *
 * 1. They only cover the ROLE half of the model. "Is this your course" and
 *    "are you enrolled" cannot be expressed as a URL pattern — those checks
 *    live in CourseAccessService, called from the services.
 *
 * 2. ORDER IS EVERYTHING. Spring applies the first rule that matches and
 *    stops. Every student action in this API sits under a path named after
 *    an instructor-owned resource, so those rules have to come first.
 *
 * 3. permitAll does NOT mean "let anyone through and carry on". It matches,
 *    the chain ends, and no authentication is established for the request.
 *    A @PreAuthorize on the controller then sees an anonymous principal and
 *    answers 403 — which is why /courses/** cannot be blanket-public while
 *    /courses/{id}/students is role-gated. Hence the narrow GET rules below.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtRoleConverter jwtRoleConverter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtRoleConverter jwtRoleConverter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtRoleConverter = jwtRoleConverter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

        // 1. REST architecture — STATELESS
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 2. Endpoint policy
        http.authorizeHttpRequests(request -> request

                // Server-Sent Events (live notifications) finish on an ASYNC dispatch; the original request was already authorized
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC, jakarta.servlet.DispatcherType.ERROR).permitAll()

                // =========================================================
                //  Docs, static pages, uploaded images
                // =========================================================
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/demo.html",
                        "/swagger-ui.html/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/scalar/**",
                        "/api/v1/auth/register",
                        "/media/**"
                ).permitAll()

                // =========================================================
                //  COURSES — the trickiest block, so it goes first.
                //
                //  Four different audiences share the /courses prefix:
                //  admins, instructors, enrolled students and anonymous
                //  visitors. Ordering decides who gets what.
                // =========================================================

                // Named paths before the {id} wildcard, or "mine" and
                // "pending-review" are read as course ids and made public.
                .requestMatchers(HttpMethod.GET, "/api/v1/courses/mine").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/courses/pending-review").hasAuthority("ADMIN")

                // Student actions nested under a course.
                .requestMatchers(HttpMethod.POST,   "/api/v1/courses/*/enroll").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*/enroll").authenticated()
                .requestMatchers(HttpMethod.GET,    "/api/v1/courses/*/quiz/take").authenticated()
                .requestMatchers(HttpMethod.GET,    "/api/v1/courses/*/quiz/edit").hasAnyAuthority("INSTRUCTOR", "ADMIN")

                // The roster is instructor data even though it lives here.
                .requestMatchers(HttpMethod.GET,    "/api/v1/courses/*/students").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*/students/*").hasAuthority("ADMIN")

                // Reviews. Reading them is part of the public catalog; writing
                // one is a student action. Both sit under /courses/{id}/, so
                // these rules MUST precede the authoring block below — PUT
                // /api/v1/courses/** would otherwise claim the write and hand
                // every student a 403.
                .requestMatchers(HttpMethod.GET,    "/api/v1/courses/*/reviews").permitAll()
                .requestMatchers(HttpMethod.GET,    "/api/v1/courses/*/reviews/summary").permitAll()
                .requestMatchers(HttpMethod.GET,    "/api/v1/courses/*/reviews/mine").authenticated()
                .requestMatchers(HttpMethod.PUT,    "/api/v1/courses/*/reviews/mine").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*/reviews/mine").authenticated()

                // Public catalog. Listed one path at a time rather than with
                // /courses/**, which would swallow the rules above.
                .requestMatchers(HttpMethod.GET, "/api/v1/courses").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/courses/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/courses/slug/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/courses/*/curriculum").permitAll()

                // Authoring. Role here, ownership in CourseServiceImpl.
                .requestMatchers(HttpMethod.POST,   "/api/v1/courses/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/courses/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/courses/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")

                // =========================================================
                //  LESSONS
                // =========================================================

                // Progress is a student action.
                .requestMatchers(HttpMethod.POST,   "/api/v1/lessons/*/complete").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/lessons/*/complete").authenticated()

                // GET /api/v1/lessons/{id}: Preview lessons are public, gated
                // ones need an enrollment, and neither is a role — so the URL
                // is open and CourseAccessService.requireCanViewContent decides.
                // Without this rule the request fell through to
                // anyRequest().authenticated() and anonymous visitors got 401
                // even for Preview lessons. A bearer token, when sent, is still
                // read — same as GET /courses/* above.
                .requestMatchers(HttpMethod.GET, "/api/v1/lessons/*").permitAll()

                // A student's own feedback, before the instructor-only rules below catch the PUT
                .requestMatchers("/api/v1/lessons/*/feedback/mine").authenticated()

                .requestMatchers(HttpMethod.POST,   "/api/v1/lessons").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/lessons/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/lessons/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/lessons/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/lesson-files/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")

                .requestMatchers("/api/v1/sections/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")

                // =========================================================
                //  QUIZZES
                // =========================================================

                // Taking a quiz is a student action.
                .requestMatchers(HttpMethod.POST, "/api/v1/quizzes/*/attempts").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/v1/quizzes/*/my-attempts").authenticated()

                // Same path, one segment different, opposite audience: this
                // one returns everybody's attempts.
                .requestMatchers(HttpMethod.GET, "/api/v1/quizzes/*/attempts").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/quizzes/*/edit").hasAnyAuthority("INSTRUCTOR", "ADMIN")

                .requestMatchers(HttpMethod.POST,   "/api/v1/quizzes").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/quizzes/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/quizzes/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers("/api/v1/questions/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")

                // =========================================================
                //  REVIEWS (top-level) and REPORTS
                //
                //  The per-course review paths are handled in the COURSES
                //  block above. What is left is the admin takedown and the
                //  dashboard numbers.
                // =========================================================
                .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/*").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/reports/**").hasAuthority("ADMIN")

                // =========================================================
                //  MODERATION, ASSESSMENTS, CERTIFICATES
                //
                //  Any signed-in user may report a review; only admins work
                //  the queue. Assessments are teaching data. A certificate is
                //  verifiable by serial without an account — that is its point.
                //  (My certificates / bookmarks live under /my/** below.)
                // =========================================================
                .requestMatchers(HttpMethod.POST, "/api/v1/reviews/*/report").authenticated()
                .requestMatchers("/api/v1/moderation/**").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/assessments/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers("/api/v1/manage/certificates").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers("/api/v1/manage/ai-access/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/certificates/*").permitAll()

                // AI tutor: signed-in users; lesson access is checked in AssistantService
                .requestMatchers(HttpMethod.GET, "/api/v1/assistant/status").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/lessons/*/assistant").authenticated()

                // =========================================================
                //  CATEGORIES — taxonomy is admin-only, reads are public
                // =========================================================
                .requestMatchers(HttpMethod.GET,    "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST,   "/api/v1/categories/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/categories/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/categories/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasAuthority("ADMIN")

                // =========================================================
                //  MEDIA — uploads are for authors, reads are public
                // =========================================================
                .requestMatchers(HttpMethod.GET,    "/api/v1/medias/*").permitAll()
                .requestMatchers(HttpMethod.GET,    "/api/v1/medias").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/v1/medias/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/medias/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/medias/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")

                // =========================================================
                //  Signed file urls. The signature IS the credential — it is
                //  issued only after the content gate passes and expires in
                //  60 seconds. A browser video element cannot send an
                //  Authorization header, so this path has to be open.
                // =========================================================
                .requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()

                // =========================================================
                //  My learning, own profile
                // =========================================================
                .requestMatchers("/api/v1/my/**").authenticated()
                .requestMatchers("/api/v1/users/me/**").authenticated()

                .anyRequest().authenticated()
        );

        // 3. Authentication mechanism
        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        // 4. CSRF off (stateless API), CORS on
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtRoleConverter);
        return converter;
    }
}