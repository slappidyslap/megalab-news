package kg.musabaev.megalabnews.controller;

import jakarta.validation.Valid;
import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import kg.musabaev.megalabnews.dto.UpdateCommentRequest;
import kg.musabaev.megalabnews.repository.projection.CommentListView;
import kg.musabaev.megalabnews.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@CrossOrigin(originPatterns = "*")
public class CommentController {

	private final CommentService commentService;

	@PostMapping("/{postId}/comments")
	@PreAuthorize("hasAuthority('WRITE_COMMENT')")
	ResponseEntity<NewOrUpdateCommentResponse> saveComment(
			@PathVariable Long postId,
			@Valid @RequestBody NewCommentRequest dto) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body((commentService.save(postId, dto)));
	}

	@GetMapping("/{postId}/comments")
	ResponseEntity<Page<CommentListView>> getRootCommentsOfPostById(
			@PathVariable Long postId,
			@PageableDefault Pageable pageable) {
		return ResponseEntity.ok(commentService.getRootsByPostId(postId, pageable));
	}

	@GetMapping("/{postId}/comments/{parentCommentId}")
	ResponseEntity<Page<CommentListView>> getCommentChildrenOfParentId(
			@PathVariable Long postId,
			@PathVariable Long parentCommentId,
			@PageableDefault Pageable pageable) {
		return ResponseEntity.ok(commentService.getChildrenByParentId(postId, parentCommentId, pageable));
	}

	@PatchMapping("/{postId}/comments/{commentId}")
	@PreAuthorize("hasAuthority('WRITE_COMMENT')")
	ResponseEntity<NewOrUpdateCommentResponse> updateCommentById(
			@PathVariable Long postId,
			@PathVariable Long commentId,
			@Valid @RequestBody UpdateCommentRequest dto) {
		return ResponseEntity.ok(commentService.update(postId, commentId, dto));
	}

	@DeleteMapping("/{postId}/comments/{commentId}")
	@PreAuthorize("hasAuthority('WRITE_COMMENT')")
	void deleteCommentById(
			@PathVariable Long postId,
			@PathVariable Long commentId) {
		commentService.deleteById(postId, commentId);
	}
}
