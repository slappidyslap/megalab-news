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
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

	public static final String childCommentsCacheName = "childCommentList";
	public static final String rootCommentsCacheName = "rootCommentList";

	private final CommentRepo commentRepo;
	private final CacheManager cacheManager;
	private final CommentMapper commentMapper;

	@Override
	@Transactional
	/*@Caching(evict = {
			@CacheEvict(
					cacheNames = childCommentsCacheName,
					condition = "#dto.parentId() != null",
					allEntries = true),
			@CacheEvict(
					cacheNames = rootCommentsCacheName,
					condition = "#dto.parentId() == null",
					allEntries = true)
	})*/
	public NewOrUpdateCommentResponse save(Long postId, NewCommentRequest dto) {
		Post post = Utils.getPostReferenceByIdOrElseThrow(postId);
		Comment parentComment = dto.parentId() != null
				? Utils.getCommentReferenceByIdOrElseThrow(postId, dto.parentId())
				: null;

		Comment newComment = commentMapper.toModel(dto);
		newComment.setParent(parentComment);
		newComment.setPost(post);
		newComment.setCommentator(0L); // TODO переделать когда будет spring security

		return commentMapper.toDto(commentRepo.save(newComment));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = rootCommentsCacheName)
	public Page<CommentListView> getRootsByPostId(Long postId, Pageable pageable) {
		Utils.assertPostExistsByIdOrElseThrow(postId);

		return commentRepo.findRootsByPostIdAndParentIsNull(postId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(childCommentsCacheName)
	public Page<CommentListView> getChildrenByParentId(Long postId, Long parentCommentId, Pageable pageable) {
		Utils.assertPostExistsByIdOrElseThrow(postId);
		Utils.assertCommentExistsByIdOrElseThrow(postId, parentCommentId);

		return commentRepo.findChildrenByParentIdAndPostId(parentCommentId, postId, pageable);
	}

	@Override
	@Transactional
	@Caching(evict = {
			@CacheEvict(cacheNames = childCommentsCacheName, condition = "#result.parentId() != null"),
			@CacheEvict(cacheNames = rootCommentsCacheName, condition = "#result.parentId() == null")
	})
	public NewOrUpdateCommentResponse update(Long postId, Long commentId, UpdateCommentRequest dto) {
		Utils.assertPostExistsByIdOrElseThrow(postId);

		Comment updatedComment = commentRepo.findByIdAndPostId(commentId, postId).map(comment -> {
			comment.setContent(dto.content());

			return commentRepo.save(comment);
		}).orElseThrow(() -> { throw new CommentNotFoundException();});
		return commentMapper.toDto(updatedComment);
	}

	@Override
	@Transactional
	@Caching(evict = {
			@CacheEvict(cacheNames = childCommentsCacheName, condition = "#result.parentId() != null"),
			@CacheEvict(cacheNames = rootCommentsCacheName, condition = "#commentRepo")
	})
	public void deleteById(Long postId, Long commentId) {
		Utils.assertCommentExistsByIdOrElseThrow(postId, commentId);
		commentRepo.deleteById(commentId);
	}
}
