package kg.musabaev.megalabnews.controller;

import kg.musabaev.megalabnews.dto.*;
import kg.musabaev.megalabnews.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class AuthenticationController {

	private final AuthenticationService authService;

	@PostMapping("/authenticate")
	ResponseEntity<AuthenticateOrRefreshResponse> authenticate(@Valid @RequestBody AuthenticateRequest dto) {
		return ResponseEntity.ok(authService.authenticate(dto));
	}

	@PostMapping("/register")
	ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest dto) {
		return ResponseEntity.ok(authService.register(dto));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthenticateOrRefreshResponse> refresh(@Valid @RequestBody UpdateTokenRequest dto) {
		return ResponseEntity.ok(authService.refresh(dto));
	}

}
