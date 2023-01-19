package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.AuthenticateRequest;
import kg.musabaev.megalabnews.dto.RegisterUserRequest;
import kg.musabaev.megalabnews.dto.UpdateTokenRequest;
import kg.musabaev.megalabnews.exception.ResponseStatusConflictException;
import kg.musabaev.megalabnews.exception.ResponseStatusNotFoundException;
import kg.musabaev.megalabnews.mapper.UserMapper;
import kg.musabaev.megalabnews.model.User;
import kg.musabaev.megalabnews.repository.RefreshTokenRepo;
import kg.musabaev.megalabnews.repository.UserRepo;
import kg.musabaev.megalabnews.security.RefreshToken;
import kg.musabaev.megalabnews.security.TokenService;
import kg.musabaev.megalabnews.service.impl.SimpleAuthenticationService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationServiceTest {

	@Mock
	UserRepo userRepo;
	@Mock
	RefreshTokenRepo refreshTokenRepo;
	@Mock
	AuthenticationManager authenticationManager;
	@Mock
	TokenService tokenService;
	@Mock
	PasswordEncoder passwordEncoder;
	@Spy
	UserMapper userMapper = Mappers.getMapper(UserMapper.class);

	@InjectMocks
	SimpleAuthenticationService service;

	@Test
	void shouldAuthenticateUser() {
		String username = "eld";
		var authenticateRequest = new AuthenticateRequest(username, "1");

		when(userRepo.findByUsername(username))
				.thenReturn(Optional.of(User.builder().username(username).build()));

		var authenticateResponse = service.authenticate(authenticateRequest);

		assertThat(authenticateResponse.user().username()).isEqualTo(username);
	}

	@Test
	void shouldRegisterUser() {
		var registerUserRequest = getRegisterUserRequest();

		when(userRepo.existsByUsername(registerUserRequest.username())).thenReturn(false);
		when(userRepo.save(any(User.class))).then(i -> i.getArgument(0));

		var registerUserResponse = service.register(registerUserRequest);

		assertThat(registerUserResponse)
				.usingRecursiveComparison()
				.comparingOnlyFields("name", "surname", "username")
				.isEqualTo(registerUserRequest);
	}

	@Test
	void shouldThrowUserExists_whenRegisteringUser() {
		var registerUserRequest = getRegisterUserRequest();
		when(userRepo.existsByUsername(registerUserRequest.username())).thenReturn(true);

		assertThatThrownBy(() -> service.register(registerUserRequest))
				.isInstanceOf(ResponseStatusConflictException.class);
	}

	@Test
	void shouldUpdateTokens() {
		String refreshTokenValue = UUID.randomUUID().toString();
		RefreshToken refreshToken = RefreshToken.builder()
				.token(refreshTokenValue)
				.expiryDate(Instant.now().plus(3, ChronoUnit.HOURS))
				.owner(new User())
				.build();
		var updateTokenRequest = new UpdateTokenRequest(refreshTokenValue);

		when(refreshTokenRepo.findByToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));

		assertThatCode(() -> service.refresh(updateTokenRequest))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldThrowRefreshTokenNotFound_whenUpdatingTokens() {
		String refreshTokenValue = UUID.randomUUID().toString();
		var updateTokenRequest = new UpdateTokenRequest(refreshTokenValue);

		when(refreshTokenRepo.findByToken(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.refresh(updateTokenRequest))
				.isInstanceOf(ResponseStatusNotFoundException.class);
	}

	@Test
	void shouldThrowExceptionByRefreshTokenExpiring() {
		String refreshTokenValue = UUID.randomUUID().toString();
		RefreshToken refreshToken = RefreshToken.builder()
				.token(refreshTokenValue)
				.expiryDate(Instant.now().minus(3, ChronoUnit.MINUTES))
				.owner(new User())
				.build();
		var updateTokenRequest = new UpdateTokenRequest(refreshTokenValue);

		when(refreshTokenRepo.findByToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));

		assertThatThrownBy(() -> service.refresh(updateTokenRequest))
				.isInstanceOf(ResponseStatusException.class);
	}


	private RegisterUserRequest getRegisterUserRequest() {
		return new RegisterUserRequest(
				"name",
				"surname",
				"username",
				"1"
		);
	}

}
