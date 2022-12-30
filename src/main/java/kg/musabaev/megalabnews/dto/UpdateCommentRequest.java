package kg.musabaev.megalabnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
		@NotNull
		@NotBlank
		@Size(max = 2000)
		String content
) {
}
