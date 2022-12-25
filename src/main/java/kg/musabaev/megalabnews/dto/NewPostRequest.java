package kg.musabaev.megalabnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NewPostRequest(
		@NotNull
		@NotBlank
		String title,
		@NotNull
		String description,
		@NotNull
		@NotBlank
		String content,
		@NotNull
		List<String> tags,
		@NotNull
		@NotBlank
		String coverPath
) {
}
