package co.istad.visal.e_learning.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Adds an Authorize button to Swagger UI so you can paste a Keycloak token
 * and exercise the gated endpoints without curl.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "E-Learning API",
                version = "v1",
                description = "Courses, enrollment and quizzes. Roles come from Keycloak; "
                        + "ownership and enrollment come from the database."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
