package kg.musabaev.megalabnews.service.impl;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import kg.musabaev.megalabnews.dto.UpdateCommentRequest;
import kg.musabaev.megalabnews.exception.CommentNotFoundException;
import kg.musabaev.megalabnews.exception.PostNotFoundException;
import kg.musabaev.megalabnews.mapper.CommentMapper;
import kg.musabaev.megalabnews.model.Comment;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.CommentRepo;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.projection.CommentListView;
import kg.musabaev.megalabnews.service.CommentService;
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

	private final PostRepo postRepo;
	private final CommentRepo commentRepo;
	private final CommentMapper commentMapper;

	@Override
	@Transactional
	public NewOrUpdateCommentResponse save(Long postId, NewCommentRequest dto) {
		Post post = getPostReferenceByIdElseThrow(postId);
		Comment parentComment = dto.parentId() != null
				? getCommentReferenceByIdElseThrow(postId, dto.parentId())
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
		assertPostExistsByIdElseThrow(postId);

		return commentRepo.findRootsByPostIdAndParentIsNull(postId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<CommentListView> getChildrenByParentId(Long postId, Long parentCommentId, Pageable pageable) {
		assertPostExistsByIdElseThrow(postId);
		assertCommentExistsByIdElseThrow(postId, parentCommentId);

		return commentRepo.findChildrenByParentIdAndPostId(parentCommentId, postId, pageable);
	}

	@Override
	@Transactional
	public NewOrUpdateCommentResponse update(Long postId, Long commentId, UpdateCommentRequest dto) {
		assertPostExistsByIdElseThrow(postId);

		Comment updatedComment = commentRepo.findByIdAndPostId(commentId, postId).map(comment -> {
			comment.setContent(dto.content());

			return commentRepo.save(comment);
		}).orElseThrow(() -> { throw new CommentNotFoundException();});
		return commentMapper.toDto(updatedComment);
	}

	@Override
	@Transactional
	public void deleteById(Long postId, Long commentId) {
		assertCommentExistsByIdElseThrow(postId, commentId);
		commentRepo.deleteById(commentId);
	}

	private Post getPostReferenceByIdElseThrow(Long postId) {
		if (!postRepo.existsById(postId)) throw new PostNotFoundException();
		return postRepo.getReferenceById(postId);
	}

	private void assertPostExistsByIdElseThrow(Long postId) {
		if (!postRepo.existsById(postId)) throw new PostNotFoundException();
	}

	private Comment getCommentReferenceByIdElseThrow(Long postId, Long commentId) {
		if (!commentRepo.existsByIdAndPostId(commentId, postId)) throw new CommentNotFoundException();
		return commentRepo.getReferenceById(commentId);
	}

	private void assertCommentExistsByIdElseThrow(Long postId, Long commentId) {
		if (!commentRepo.existsByIdAndPostId(commentId, postId)) throw new CommentNotFoundException();
	}
}
