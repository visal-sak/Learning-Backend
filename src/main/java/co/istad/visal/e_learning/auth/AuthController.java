package co.istad.visal.e_learning.auth;

import co.istad.visal.e_learning.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Three endpoints, and each is here only because it needs the Keycloak admin
 * service account — something a public browser client cannot hold.
 *
 * Login, refresh and logout are deliberately absent. Authentication is
 * Authorization Code + PKCE: the browser talks to Keycloak directly, and this
 * application is a pure resource server that validates tokens and never sees
 * a password.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

}