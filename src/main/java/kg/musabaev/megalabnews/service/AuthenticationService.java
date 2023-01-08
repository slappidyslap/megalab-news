package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.*;

public interface AuthenticationService {

	AuthenticateOrRefreshResponse authenticate(AuthenticateRequest request);

	AuthenticateOrRefreshResponse refresh(UpdateTokenRequest request);

	RegisterUserResponse register(RegisterUserRequest request);
}
