package co.istad.visal.e_learning.features.user;

import co.istad.visal.e_learning.features.user.dto.AssignRoleRequest;
import co.istad.visal.e_learning.features.user.dto.PublicProfileResponse;
import co.istad.visal.e_learning.features.user.dto.UpdateProfileRequest;
import co.istad.visal.e_learning.features.user.dto.UserResponse;
import co.istad.visal.e_learning.features.user.dto.UserAccountResponse;
import co.istad.visal.e_learning.features.user.dto.UserRolesResponse;
import co.istad.visal.e_learning.utils.PageResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    @PreAuthorize("hasAuthority('ADMIN')")
    public UserResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    /** Every account in the realm, for the admin "User roles" table. ?q= matches name, username or email. */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public PageResponse<UserAccountResponse> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listAccounts(q, pageable);
    }

    /**
     * Promote or demote a user, e.g. { "email": "...", "role": "INSTRUCTOR" }.
     * The change reaches the user's token on their next refresh or login.
     */
    @PutMapping("/roles")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UserRolesResponse assignRole(@Valid @RequestBody AssignRoleRequest request) {
        return service.assignRole(request);
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
