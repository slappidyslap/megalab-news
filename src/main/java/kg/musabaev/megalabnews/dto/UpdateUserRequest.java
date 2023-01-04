package kg.musabaev.megalabnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kg.musabaev.megalabnews.util.validator.UrlOrNull;

public record UpdateUserRequest(
		@NotNull
		@NotBlank
		String name,
		@NotNull
		@NotBlank
		String surname,
		@NotNull
		@NotBlank
		String username,
		@UrlOrNull
		@Size(max = 2000)
		String userPictureUrl
) {
}
