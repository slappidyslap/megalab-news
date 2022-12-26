package kg.musabaev.megalabnews.dto;

import java.time.LocalDate;
import java.util.Set;

public record NewOrUpdatePostResponse(
		Long id,
		String title,
		String description,
		LocalDate createdDate,
		Set<String> tags,
		String content,
		String imageUrl
) {
}
