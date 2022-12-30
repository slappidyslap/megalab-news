package kg.musabaev.megalabnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record NewCommentRequest(
		@Nullable
		@Positive
		Long parentId,
		@NotNull
		@NotBlank
		@Size(max = 2000)
		String content
) {
}
