package kg.musabaev.megalabnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthenticateRequest(
		@NotNull
		@NotBlank
		String username,
		@NotNull
		@NotBlank
		String password
) {
}
