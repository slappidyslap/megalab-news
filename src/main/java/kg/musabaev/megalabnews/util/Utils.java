package kg.musabaev.megalabnews.util;

import jakarta.annotation.PostConstruct;
import kg.musabaev.megalabnews.exception.CommentNotFoundException;
import kg.musabaev.megalabnews.exception.PostNotFoundException;
import kg.musabaev.megalabnews.model.Comment;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.CommentRepo;
import kg.musabaev.megalabnews.repository.PostRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/*
Если необходимо перенести в другой класс
Refactor -> Move static member
*/
@Component
@Log4j2
@RequiredArgsConstructor
public class Utils {

	private final PostRepo _postRepo;
	private final CommentRepo _commentRepo;

	private static PostRepo postRepo;
	private static CommentRepo commentRepo;

	@PostConstruct
	public void init() {
		postRepo = _postRepo;
		commentRepo = _commentRepo;
	}

	public static Post getPostReferenceByIdElseThrow(Long postId) {
		if (!postRepo.existsById(postId)) throw new PostNotFoundException();
		return postRepo.getReferenceById(postId);
	}

	public static void assertPostExistsByIdElseThrow(Long postId) {
		if (!postRepo.existsById(postId)) throw new PostNotFoundException();
	}

	public static Comment getCommentReferenceByIdElseThrow(Long postId, Long commentId) {
		if (!commentRepo.existsByIdAndPostId(commentId, postId))
			throw new CommentNotFoundException();
		return commentRepo.getReferenceById(commentId);
	}

	public static void assertCommentExistsByIdElseThrow(Long postId, Long commentId) {
		if (!commentRepo.existsByIdAndPostId(commentId, postId))
			throw new CommentNotFoundException();
	}

	public static boolean ifInternalServerError(Exception e, Runnable runnable) {
		return ifResponseStatusExceptionWithStatusElseLog(e, INTERNAL_SERVER_ERROR, runnable);
	}

	public static boolean ifNotFound(Exception e, Runnable runnable) {
		return ifResponseStatusExceptionWithStatusElseLog(e, NOT_FOUND, runnable);
	}

	public static boolean ifResponseStatusExceptionWithStatusElseLog(Exception e, HttpStatus status, Runnable runnable) {
		if (e.getClass() == ResponseStatusException.class &&
				((ResponseStatusException) e).getStatusCode() == HttpStatusCode.valueOf(status.value())) {
			runnable.run();
			return true;
		}
		log.warn("Произошло другое исключение:", e);
		return false;
	}
}