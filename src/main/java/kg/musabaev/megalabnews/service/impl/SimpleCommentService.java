package kg.musabaev.megalabnews.service.impl;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import kg.musabaev.megalabnews.dto.UpdateCommentRequest;
import kg.musabaev.megalabnews.exception.CommentNotFoundException;
import kg.musabaev.megalabnews.mapper.CommentMapper;
import kg.musabaev.megalabnews.model.Comment;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.CommentRepo;
import kg.musabaev.megalabnews.repository.projection.CommentListView;
import kg.musabaev.megalabnews.service.CommentService;
import kg.musabaev.megalabnews.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Primary
@Log4j2
public class SimpleCommentService implements CommentService {

	private final CommentRepo commentRepo;
	private final CommentMapper commentMapper;

	@Override
	@Transactional
	public NewOrUpdateCommentResponse save(Long postId, NewCommentRequest dto) {
		Post post = Utils.getPostReferenceByIdElseThrow(postId);
		Comment parentComment = dto.parentId() != null
				? Utils.getCommentReferenceByIdElseThrow(postId, dto.parentId())
				: null;

		Comment newComment = commentMapper.toModel(dto);
		newComment.setParent(parentComment);
		newComment.setPost(post);
		newComment.setCommentator(0L); // TODO переделать когда будет spring security

		return commentMapper.toDto(commentRepo.save(newComment));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<CommentListView> getRootsByPostId(Long postId, Pageable pageable) {
		Utils.assertPostExistsByIdElseThrow(postId);

		return commentRepo.findRootsByPostIdAndParentIsNull(postId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<CommentListView> getChildrenByParentId(Long postId, Long parentCommentId, Pageable pageable) {
		Utils.assertPostExistsByIdElseThrow(postId);
		Utils.assertCommentExistsByIdElseThrow(postId, parentCommentId);

		return commentRepo.findChildrenByParentIdAndPostId(parentCommentId, postId, pageable);
	}

	@Override
	@Transactional
	public NewOrUpdateCommentResponse update(Long postId, Long commentId, UpdateCommentRequest dto) {
		Utils.assertPostExistsByIdElseThrow(postId);

		Comment updatedComment = commentRepo.findByIdAndPostId(commentId, postId).map(comment -> {
			comment.setContent(dto.content());

			return commentRepo.save(comment);
		}).orElseThrow(() -> { throw new CommentNotFoundException();});
		return commentMapper.toDto(updatedComment);
	}

	@Override
	@Transactional
	public void deleteById(Long postId, Long commentId) {
		Utils.assertCommentExistsByIdElseThrow(postId, commentId);
		commentRepo.deleteById(commentId);
	}
}
