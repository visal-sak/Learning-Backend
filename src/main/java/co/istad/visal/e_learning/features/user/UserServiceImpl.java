package co.istad.visal.e_learning.features.user;

import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.security.CurrentUserService;
import co.istad.visal.e_learning.features.user.dto.PublicProfileResponse;
import co.istad.visal.e_learning.features.user.dto.UpdateProfileRequest;
import co.istad.visal.e_learning.features.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final CurrentUserService currentUser;

    public UserServiceImpl(UserRepository repository, UserMapper mapper, CurrentUserService currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Override
    public UserResponse getCurrentProfile() {
        return mapper.toResponse(currentUser.require());
    }

    @Override
    @Transactional
    public UserResponse updateCurrentProfile(UpdateProfileRequest request) {
        User user = currentUser.require();
        mapper.updateEntity(request, user);
        return mapper.toResponse(user);
    }

    @Override
    public PublicProfileResponse getPublicProfile(UUID id) {
        return mapper.toPublicProfile(load(id));
    }

    @Override
    public UserResponse getById(UUID id) {
        return mapper.toResponse(load(id));
    }

    private User load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No user with id " + id));
    }
}
