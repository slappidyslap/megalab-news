package kg.musabaev.megalabnews.dto;

import java.time.LocalDate;

public record NewOrUpdateCommentResponse(
		Long id,
		Long postId,
		Long parentId,
		Long commentatorId,
		String content,
		LocalDate createdDate
) {
}
