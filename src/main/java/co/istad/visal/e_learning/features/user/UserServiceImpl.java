package co.istad.visal.e_learning.features.user;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.security.AuthUtils;
import co.istad.visal.e_learning.security.CurrentUserService;
import co.istad.visal.e_learning.security.KeycloakUserService;
import co.istad.visal.e_learning.features.user.dto.AssignRoleRequest;
import co.istad.visal.e_learning.features.user.dto.PublicProfileResponse;
import co.istad.visal.e_learning.features.user.dto.UpdateProfileRequest;
import co.istad.visal.e_learning.features.user.dto.UserResponse;
import co.istad.visal.e_learning.features.user.dto.UserAccountResponse;
import co.istad.visal.e_learning.features.user.dto.UserRolesResponse;
import co.istad.visal.e_learning.utils.PageResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final int MAX_ACCOUNT_PAGE_SIZE = 50;

    private final UserRepository repository;
    private final UserMapper mapper;
    private final CurrentUserService currentUser;
    private final KeycloakUserService keycloakUsers;

    public UserServiceImpl(UserRepository repository, UserMapper mapper, CurrentUserService currentUser,
                           KeycloakUserService keycloakUsers) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.keycloakUsers = keycloakUsers;
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

    /**
     * Roles are one Keycloak call per user, so the page size is capped. The
     * table asks for 20 at a time.
     */
    @Override
    public PageResponse<UserAccountResponse> listAccounts(String q, Pageable pageable) {
        String search = q == null || q.isBlank() ? null : q.trim();
        int size = Math.min(pageable.getPageSize(), MAX_ACCOUNT_PAGE_SIZE);
        int page = pageable.getPageNumber();

        long total = keycloakUsers.countUsers(search);
        List<UserAccountResponse> content = withRoles(keycloakUsers.listUsers(search, page * size, size));

        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages, page + 1 >= totalPages);
    }

    /**
     * Admins cannot change their own role. That also guarantees the realm
     * always keeps an admin: the caller is one, and they stay one.
     */
    @Override
    public UserRolesResponse assignRole(AssignRoleRequest request) {
        UserRepresentation target = keycloakUsers.findByEmail(request.email());

        if (target.getId().equals(AuthUtils.extractUserId())) {
            throw new ConflictException("You cannot change your own role");
        }

        keycloakUsers.replaceRole(target.getId(), request.role());
        return new UserRolesResponse(target.getEmail(), keycloakUsers.rolesOf(target.getId()));
    }

    /** One Keycloak call per user, made side by side so a full page does not wait for them in a row. */
    private List<UserAccountResponse> withRoles(List<UserRepresentation> accounts) {
        List<Future<UserAccountResponse>> rows;
        // Closing the executor waits for every call, so the results are ready below
        try (var calls = Executors.newVirtualThreadPerTaskExecutor()) {
            rows = accounts.stream().map(account -> calls.submit(() -> toAccount(account))).toList();
        }
        return rows.stream().map(Future::resultNow).toList();
    }

    private UserAccountResponse toAccount(UserRepresentation account) {
        String name = Stream.of(account.getFirstName(), account.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));

        return new UserAccountResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                name.isEmpty() ? account.getUsername() : name,
                Boolean.TRUE.equals(account.isEnabled()),
                keycloakUsers.rolesOf(account.getId()));
    }

    private User load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No user with id " + id));
    }
}
