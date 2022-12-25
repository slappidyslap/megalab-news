package kg.musabaev.megalabnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
		@NotBlank
		String imageFilename
) {
}
