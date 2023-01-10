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
		String imageUrl,
		UserInfo author
) {
	public record UserInfo(
			Long id,
			String name,
			String surname,
			String username,
			String userPictureUrl
	) {
	}
}
