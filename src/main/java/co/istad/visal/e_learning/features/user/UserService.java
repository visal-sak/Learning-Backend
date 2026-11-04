package co.istad.visal.e_learning.features.user;

import co.istad.visal.e_learning.features.user.dto.AssignRoleRequest;
import co.istad.visal.e_learning.features.user.dto.PublicProfileResponse;
import co.istad.visal.e_learning.features.user.dto.UpdateProfileRequest;
import co.istad.visal.e_learning.features.user.dto.UserResponse;
import co.istad.visal.e_learning.features.user.dto.UserAccountResponse;
import co.istad.visal.e_learning.features.user.dto.UserRolesResponse;
import co.istad.visal.e_learning.utils.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentProfile();

    UserResponse updateCurrentProfile(UpdateProfileRequest request);

    PublicProfileResponse getPublicProfile(UUID id);

    /** Admin only. */
    UserResponse getById(UUID id);

    /** Admin only. Accounts come from Keycloak, so people who have never logged in are listed too. */
    PageResponse<UserAccountResponse> listAccounts(String q, Pageable pageable);

    /** Admin only. Replaces the user's STUDENT / INSTRUCTOR / ADMIN role in Keycloak. */
    UserRolesResponse assignRole(AssignRoleRequest request);
}
