package kg.musabaev.megalabnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kg.musabaev.megalabnews.util.validator.UrlOrNull;
import lombok.Builder;

import java.util.Set;

@Builder
public record NewOrUpdatePostRequest(
		@NotNull
		@NotBlank
		String title,
		@NotNull
		@Size(max = 500)
		String description,
		@NotNull
		@NotBlank
		String content,
		@NotNull
		Set<String> tags,
		@UrlOrNull
		@Size(max = 2000)
		String imageUrl
) {
}
