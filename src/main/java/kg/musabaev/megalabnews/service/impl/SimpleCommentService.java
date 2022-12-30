package kg.musabaev.megalabnews.service.impl;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import kg.musabaev.megalabnews.dto.UpdateCommentRequest;
import kg.musabaev.megalabnews.mapper.CommentMapper;
import kg.musabaev.megalabnews.model.Comment;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.CommentRepo;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.projection.NonRootCommentListView;
import kg.musabaev.megalabnews.repository.projection.RootCommentListView;
import kg.musabaev.megalabnews.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

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
		boolean isPostExistsById = postRepo.existsById(postId);
		if (!isPostExistsById) throw new ResponseStatusException(NOT_FOUND, "post not found");
		Post post = postRepo.getReferenceById(postId);

		Comment parentComment = null;
		if (dto.parentId() != null) {
			boolean isParentCommentExistsByIdInPost =
					commentRepo.existsByIdAndPostId(dto.parentId(), postId);
			if (!isParentCommentExistsByIdInPost)
				throw new ResponseStatusException(NOT_FOUND, "parent comment not found");
			parentComment = commentRepo.getReferenceById(dto.parentId());
		}

		Comment newComment = new Comment();
		newComment.setParent(parentComment);
		newComment.setPost(post);
		newComment.setCommentator(0L); // TODO переделать когда будет spring security
		newComment.setContent(dto.content());

		return commentMapper.toDto(commentRepo.save(newComment));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<RootCommentListView> getRootsByPostId(Long postId, Pageable pageable) {
		boolean isPostExistsById = postRepo.existsById(postId);
		if (!isPostExistsById) throw new ResponseStatusException(NOT_FOUND, "post not found");

		return commentRepo.findRootsByPostIdAndParentIsNull(postId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<NonRootCommentListView> getChildrenByParentId(Long postId, Long parentCommentId, Pageable pageable) {
		boolean isPostExistsById = postRepo.existsById(postId);
		if (!isPostExistsById) throw new ResponseStatusException(NOT_FOUND, "post not found");
		boolean isCommentExists = commentRepo.existsByIdAndPostId(parentCommentId, postId);
		if (!isCommentExists) throw new ResponseStatusException(NOT_FOUND, "comment not found");

		return commentRepo.findChildrenByParentIdAndPostId(parentCommentId, postId, pageable);
	}

	@Override
	@Transactional
	public NewOrUpdateCommentResponse update(Long postId, Long commentId, UpdateCommentRequest dto) {
		boolean isPostExistsById = postRepo.existsById(postId);
		if (!isPostExistsById) throw new ResponseStatusException(NOT_FOUND, "post not found");

		Comment updatedComment = commentRepo.findByIdAndPostId(commentId, postId).map(comment -> {
			comment.setContent(dto.content());

			return commentRepo.save(comment);
		}).orElseThrow(() -> {
			throw new ResponseStatusException(NOT_FOUND, "comment not found");
		});
		return commentMapper.toDto(updatedComment);
	}

	@Override
	@Transactional
	public void deleteById(Long postId, Long commentId) {
		boolean isCommentExists = commentRepo.existsByIdAndPostId(commentId, postId);
		if (!isCommentExists) throw new ResponseStatusException(NOT_FOUND);
		commentRepo.deleteById(commentId);
	}
}
