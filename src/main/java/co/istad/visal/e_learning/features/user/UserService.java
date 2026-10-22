package co.istad.visal.e_learning.features.user;

import co.istad.visal.e_learning.features.user.dto.PublicProfileResponse;
import co.istad.visal.e_learning.features.user.dto.UpdateProfileRequest;
import co.istad.visal.e_learning.features.user.dto.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentProfile();

    UserResponse updateCurrentProfile(UpdateProfileRequest request);

    PublicProfileResponse getPublicProfile(UUID id);

    /** Admin only. */
    UserResponse getById(UUID id);
}
