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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Primary
@Log4j2
public class SimpleCommentService implements CommentService {

	public static final String CHILD_COMMENTS_CACHE_NAME = "childCommentList";
	public static final String ROOT_COMMENTS_CACHE_NAME = "rootCommentList";

	private final CommentRepo commentRepo;
	private final PostRepo postRepo;
	private final CommentMapper commentMapper;

	@Override
	@Transactional
	public NewOrUpdateCommentResponse save(Long postId, NewCommentRequest dto) {
		Post post = getPostReferenceByIdOrElseThrow(postId);
		Comment parentComment = dto.parentId() != null
				? getCommentReferenceByIdOrElseThrow(postId, dto.parentId())
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
		assertPostExistsByIdOrElseThrow(postId);

		return commentRepo.findRootsByPostIdAndParentIsNull(postId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CHILD_COMMENTS_CACHE_NAME, keyGenerator = "childCommentCacheKeyGenerator")
	public Page<CommentListView> getChildrenByParentId(Long postId, Long parentCommentId, Pageable pageable) {
		assertPostExistsByIdOrElseThrow(postId);
		assertCommentExistsByIdOrElseThrow(postId, parentCommentId);

		return commentRepo.findChildrenByParentIdAndPostId(parentCommentId, postId, pageable);
	}

	@Override
	@Transactional
	public NewOrUpdateCommentResponse update(Long postId, Long commentId, UpdateCommentRequest dto) {
		assertPostExistsByIdOrElseThrow(postId);

		Comment updatedComment = commentRepo.findByIdAndPostId(commentId, postId).map(comment -> {
			comment.setContent(dto.content());

			return commentRepo.save(comment);
		}).orElseThrow(CommentNotFoundException::new);

		return commentMapper.toDto(updatedComment);
	}

	@Override
	@Transactional
	public void deleteById(Long postId, Long commentId) {
		assertCommentExistsByIdOrElseThrow(postId, commentId);
		deleteCommentsRecursively(postId, commentRepo.getAllChildCommentIdByParentId(postId, commentId));

		commentRepo.deleteById(commentId);
	}

	private Post getPostReferenceByIdOrElseThrow(Long postId) {
		assertPostExistsByIdOrElseThrow(postId);
		return postRepo.getReferenceById(postId);
	}

	private void assertPostExistsByIdOrElseThrow(Long postId) {
		if (!postRepo.existsById(postId)) throw new PostNotFoundException();
	}

	private Comment getCommentReferenceByIdOrElseThrow(Long postId, Long commentId) {
		assertCommentExistsByIdOrElseThrow(postId, commentId);
		return commentRepo.getReferenceById(commentId);
	}

	private void assertCommentExistsByIdOrElseThrow(Long postId, Long commentId) {
		if (!commentRepo.existsByIdAndPostId(commentId, postId)) throw new CommentNotFoundException();
	}

	private void deleteCommentsRecursively(Long postId, List<Long> commentsId) {
		if (commentsId.isEmpty()) return;
		for (Long commentId : commentsId) {
			deleteCommentsRecursively(
					postId, commentRepo.getAllChildCommentIdByParentId(postId, commentId));
			commentRepo.deleteById(commentId);
		}
	}
}
