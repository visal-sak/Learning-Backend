package co.istad.visal.e_learning.features.user.dto;

import java.util.List;

public record UserRolesResponse(
        String email,
        List<String> roles
) {
}
