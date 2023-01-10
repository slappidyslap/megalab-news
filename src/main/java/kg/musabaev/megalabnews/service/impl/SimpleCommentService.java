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
import org.springframework.cache.annotation.Cacheable;
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

	public static final String CHILD_COMMENTS_CACHE_NAME = "childCommentList";
	public static final String ROOT_COMMENTS_CACHE_NAME = "rootCommentList";

	private final CommentRepo commentRepo;
	private final CommentMapper commentMapper;

	@Override
	@Transactional
	public NewOrUpdateCommentResponse save(Long postId, NewCommentRequest dto) {
		Post post = Utils.getPostReferenceByIdOrElseThrow(postId);
		Comment parentComment = dto.parentId() != null
				? Utils.getCommentReferenceByIdOrElseThrow(postId, dto.parentId())
				: null;

		Comment newComment = commentMapper.toModel(dto);
		newComment.setParent(parentComment);
		newComment.setPost(post);

		return commentMapper.toDto(commentRepo.save(newComment));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = ROOT_COMMENTS_CACHE_NAME, keyGenerator = "pairCacheKeyGenerator")
	public Page<CommentListView> getRootsByPostId(Long postId, Pageable pageable) {
		Utils.assertPostExistsByIdOrElseThrow(postId);

		return commentRepo.findRootsByPostIdAndParentIsNull(postId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CHILD_COMMENTS_CACHE_NAME, keyGenerator = "childCommentCacheKeyGenerator")
	public Page<CommentListView> getChildrenByParentId(Long postId, Long parentCommentId, Pageable pageable) {
		Utils.assertPostExistsByIdOrElseThrow(postId);
		Utils.assertCommentExistsByIdOrElseThrow(postId, parentCommentId);

		return commentRepo.findChildrenByParentIdAndPostId(parentCommentId, postId, pageable);
	}

	@Override
	@Transactional
	public NewOrUpdateCommentResponse update(Long postId, Long commentId, UpdateCommentRequest dto) {
		Utils.assertPostExistsByIdOrElseThrow(postId);

		Comment updatedComment = commentRepo.findByIdAndPostId(commentId, postId).map(comment -> {
			comment.setContent(dto.content());

			return commentRepo.save(comment);
		}).orElseThrow(() -> {
			throw new CommentNotFoundException();
		});
		return commentMapper.toDto(updatedComment);
	}

	@Override
	@Transactional
	public void deleteById(Long postId, Long commentId) {
		Utils.assertCommentExistsByIdOrElseThrow(postId, commentId);
		Utils.deleteCommentsRecursively(postId, commentRepo.getAllChildCommentIdByParentId(postId, commentId));

		commentRepo.deleteById(commentId);
	}
}
