package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import kg.musabaev.megalabnews.dto.UpdateCommentRequest;
import kg.musabaev.megalabnews.repository.projection.NonRootCommentListView;
import kg.musabaev.megalabnews.repository.projection.RootCommentListView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

	NewOrUpdateCommentResponse save(Long postId, NewCommentRequest dto);

	Page<RootCommentListView> getRootsByPostId(Long postId, Pageable pageable);

	Page<NonRootCommentListView> getChildrenByParentId(Long parentCommentId, Long commentId, Pageable pageable);

	NewOrUpdateCommentResponse update(Long postId, Long commentId, UpdateCommentRequest dto);

	void deleteById(Long postId, Long commentId);
}
