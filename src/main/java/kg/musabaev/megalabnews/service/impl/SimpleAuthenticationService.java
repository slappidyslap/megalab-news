package kg.musabaev.megalabnews.service.impl;

import kg.musabaev.megalabnews.dto.*;
import kg.musabaev.megalabnews.exception.UserNotFoundException;
import kg.musabaev.megalabnews.mapper.UserMapper;
import kg.musabaev.megalabnews.model.User;
import kg.musabaev.megalabnews.repository.RefreshTokenRepo;
import kg.musabaev.megalabnews.repository.UserRepo;
import kg.musabaev.megalabnews.security.Authority;
import kg.musabaev.megalabnews.security.RefreshToken;
import kg.musabaev.megalabnews.security.TokenService;
import kg.musabaev.megalabnews.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Primary
@Log4j2
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SimpleAuthenticationService implements AuthenticationService {

	RefreshTokenRepo refreshTokenRepo;
	UserRepo userRepo;
	TokenService tokenService;
	AuthenticationManager authenticationManager;
	PasswordEncoder passwordEncoder;
	UserMapper userMapper;

	@Override
	@Transactional(readOnly = true)
	public AuthenticateOrRefreshResponse authenticate(AuthenticateRequest request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password()));
		User foundUser = userRepo.findByUsername(request.username()).orElseThrow(() -> {
			throw new UserNotFoundException();
		});
		return buildAuthenticationResponse(foundUser);
	}

	@Override
	@Transactional
	public AuthenticateOrRefreshResponse refresh(UpdateTokenRequest request) {
		RefreshToken refreshToken = refreshTokenRepo.findByToken(request.refreshToken()).orElseThrow(() -> {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		});
		refreshTokenRepo.deleteById(refreshToken.getId());

		if (Instant.now().isAfter(refreshToken.getExpiryDate()))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh token has expired");

		return buildAuthenticationResponse(refreshToken.getOwner());
	}

	@Override
	@Transactional
	public RegisterUserResponse register(RegisterUserRequest request) {
		if (userRepo.existsByUsername(request.username()))
			throw new ResponseStatusException(HttpStatus.CONFLICT);

		User newUser = userMapper.toModel(request);
		newUser.setPassword(passwordEncoder.encode(request.password()));
		newUser.setAuthorities(getDefaultAuthorities());

		return userMapper.toRegisterUserDto(userRepo.save(newUser));
	}

	private Set<Authority> getDefaultAuthorities() {
		return Set.of(
				Authority.READ_POST, Authority.WRITE_POST,
				Authority.READ_COMMENT, Authority.WRITE_COMMENT,
				Authority.READ_USER, Authority.WRITE_USER);
	}

	private AuthenticateOrRefreshResponse buildAuthenticationResponse(User user) {
		return new AuthenticateOrRefreshResponse(
				tokenService.generateAccessToken(user.getUsername()),
				tokenService.generateRefreshToken(user.getId()),
				userMapper.toAuthResponseUserDto(user)
		);
	}
}
