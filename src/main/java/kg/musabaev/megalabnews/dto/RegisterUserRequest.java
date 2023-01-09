package kg.musabaev.megalabnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterUserRequest(
		@NotNull
		@NotBlank
		String name,
		@NotNull
		@NotBlank
		String surname,
		@NotNull
		@NotBlank
		String username,
		@NotNull
		@NotBlank
		String password
) {
}
