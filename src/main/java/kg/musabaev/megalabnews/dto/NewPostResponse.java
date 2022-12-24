package kg.musabaev.megalabnews.dto;

import java.time.LocalDate;
import java.util.List;

public record NewPostResponse(
		Long id,
		String title,
		String description,
		LocalDate createdDate,
		List<String> tags,
		String content
) {
}
