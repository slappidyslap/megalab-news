package kg.musabaev.megalabnews.dto;

import java.time.LocalDate;

public record NewOrUpdateCommentResponse(
		Long id,
		Long postId,
		Long parentId,
		UserInfo author,
		String content,
		LocalDate createdDate
) {
	public record UserInfo(
			String id,
			String name,
			String surname,
			String username,
			String userPictureUrl
	) {
	}
}
