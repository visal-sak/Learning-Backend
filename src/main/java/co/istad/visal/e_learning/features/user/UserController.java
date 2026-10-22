package co.istad.visal.e_learning.features.user;

import co.istad.visal.e_learning.features.user.dto.PublicProfileResponse;
import co.istad.visal.e_learning.features.user.dto.UpdateProfileRequest;
import co.istad.visal.e_learning.features.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService service;

    /** Set app.keycloak.account-url in application.yml to match your realm. */
    @Value("${app.keycloak.account-url}")
    private String accountConsoleUrl;

    public UserController(UserService service) {
        this.service = service;
    }

    /** Own profile. The id comes from the token, never from the path. */
    @GetMapping("/me")
    public UserResponse me() {
        return service.getCurrentProfile();
    }

    @PutMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return service.updateCurrentProfile(request);
    }

    /** Instructor bylines and similar. No email in the response. */
    @GetMapping("/{id}/profile")
    public PublicProfileResponse publicProfile(@PathVariable UUID id) {
        return service.getPublicProfile(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    /**
     * Password changes are not implemented here on purpose. Storing or
     * proxying credentials would duplicate what Keycloak already owns; the
     * client is sent to the account console instead.
     */
    @GetMapping("/me/password")
    public Map<String, String> passwordChangeLocation() {
        return Map.of("redirectTo", accountConsoleUrl + "/#/security/signingin");
    }
}
