package kg.musabaev.megalabnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kg.musabaev.megalabnews.util.validator.UrlOrNull;

import java.util.Set;

public record NewOrUpdatePostRequest(
		@NotNull
		@NotBlank
		String title,
		@NotNull
		String description,
		@NotNull
		@NotBlank
		String content,
		@NotNull
		Set<String> tags,
		@UrlOrNull
		String imageUrl
) {
}
