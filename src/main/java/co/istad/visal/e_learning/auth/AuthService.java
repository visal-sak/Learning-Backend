package co.istad.visal.e_learning.auth;

import co.istad.visal.e_learning.auth.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest registerRequest);

}
