package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import kg.musabaev.megalabnews.dto.UpdateCommentRequest;

public interface CommentService {

	NewOrUpdateCommentResponse save(Long postId, NewCommentRequest dto);

	NewOrUpdateCommentResponse update(Long postId, Long commentId, UpdateCommentRequest dto);

	void deleteById(Long postId, Long commentId);
}
