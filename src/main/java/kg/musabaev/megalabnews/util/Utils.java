package kg.musabaev.megalabnews.util;

import jakarta.annotation.PostConstruct;
import kg.musabaev.megalabnews.controller.PostController;
import kg.musabaev.megalabnews.exception.CommentNotFoundException;
import kg.musabaev.megalabnews.exception.PostNotFoundException;
import kg.musabaev.megalabnews.exception.UserNotFoundException;
import kg.musabaev.megalabnews.model.Comment;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.CommentRepo;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.springframework.http.HttpStatus.*;

/*
Если необходимо перенести в другой класс
Refactor -> Move static member
*/
@Component
@Log4j2
@RequiredArgsConstructor
public class Utils {

	public static final Set<String> validImageFormats = Set.of(
			MediaType.IMAGE_JPEG_VALUE,
			MediaType.IMAGE_PNG_VALUE
	);

	private static PostRepo postRepo;
	private static CommentRepo commentRepo;
	private static UserRepo userRepo;

	private final PostRepo _postRepo;
	private final CommentRepo _commentRepo;
	private final UserRepo _userRepo;

	public static Post getPostReferenceByIdOrElseThrow(Long postId) {
		if (!postRepo.existsById(postId)) throw new PostNotFoundException();
		return postRepo.getReferenceById(postId);
	}

	public static void assertPostExistsByIdOrElseThrow(Long postId) {
		if (!postRepo.existsById(postId)) throw new PostNotFoundException();
	}

	public static Comment getCommentReferenceByIdOrElseThrow(Long postId, Long commentId) {
		if (!commentRepo.existsByIdAndPostId(commentId, postId))
			throw new CommentNotFoundException();
		return commentRepo.getReferenceById(commentId);
	}

	public static void assertCommentExistsByIdOrElseThrow(Long postId, Long commentId) {
		if (!commentRepo.existsByIdAndPostId(commentId, postId))
			throw new CommentNotFoundException();
	}

	public static void assertUserExistsByIdOrElseThrow(Long userId) {
		if (!userRepo.existsById(userId))
			throw new UserNotFoundException();
	}

	public static void deleteCommentsRecursively(Long postId, List<Long> commentsId) {
		if (commentsId.isEmpty()) return;
		for (Long commentId : commentsId) {
			deleteCommentsRecursively(
					postId, commentRepo.getAllChildCommentIdByParentId(postId, commentId));
			commentRepo.deleteById(commentId);
		}
	}

	public static Resource getUploadedFileByFilenameInStorage(String filename, Path storage) {
		try {
			var image = new UrlResource(storage.resolve(filename).toUri());

			if (!image.exists() || !image.isReadable()) {
				throw new ResponseStatusException(NOT_FOUND);
			}
			return image;
		} catch (MalformedURLException e) {
			throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "", e);
		}
	}

	public static String uploadFileAndGetUrlFromMethodName(
			MultipartFile file,
			Path storage,
			Class<?> controller,
			String methodName) {
		if (!isValidImageFormat(file)) throw new ResponseStatusException(BAD_REQUEST);
		String uniqueFilename = getUniqueFilename(file);
		Path pathToSave = storage.resolve(uniqueFilename);
		String fileUrl = buildUrlForImageByMethodName(controller, methodName, uniqueFilename);
		try {
			file.transferTo(pathToSave);
		} catch (IOException e) {
			throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "", e);
		}
		return fileUrl;
	}

	public static String getLastPathSegmentOrNull(String url) {
		return url != null ? url.substring(url.lastIndexOf("/") + 1) : null;
	}

	public static void deleteFileFromStorageIfExists(
			String filename,
			Path storage,
			Runnable onCompleteAction,
			Consumer<Exception> onErrorAction) {
		if (filename == null) return;
		try {
			boolean isDeleted = Files.deleteIfExists(storage.resolve(filename));

			if (isDeleted) onCompleteAction.run();
		} catch (IOException e) {
			onErrorAction.accept(e);
		}
	}

	/**
	 * Итерируется по checks и когда находит true,
	 * метод заканчивает свое выполнение
	 * (какое-то ожидаемое исключение было сгенерировано),
	 * иначе в логи напишет, что произошло другое исключение
	 *
	 * @param actualException действительное исключение
	 * @param checks          список результатов проверок, в которых произошло ли исключение или нет
	 */
	public static void iterateChainOfChecks(
			Exception actualException,
			List<Boolean> checks) {
		occurredAnotherException:
		{
			for (boolean check : checks)
				if (check) break occurredAnotherException;
			log.warn("Произошло другое исключение:", actualException);
		}
	}

	public static boolean ifCommentNotFound(Exception e, Runnable runnable) {
		return ifExceptionEqualsOrElseLog(e, CommentNotFoundException.class, runnable);
	}

	public static boolean ifPostNotFound(Exception e, Runnable runnable) {
		return ifExceptionEqualsOrElseLog(e, PostNotFoundException.class, runnable);
	}

	public static boolean ifInternalServerError(Exception e, Runnable runnable) {
		return ifResponseStatusExceptionWithStatusOrElseLog(e, INTERNAL_SERVER_ERROR, runnable);
	}

	public static boolean ifNotFound(Exception e, Runnable runnable) {
		return ifResponseStatusExceptionWithStatusOrElseLog(e, NOT_FOUND, runnable);
	}

	public static boolean ifExceptionEqualsOrElseLog(
			Exception throwing,
			Class<? extends Exception> exceptedException,
			Runnable runnable) {
		if (exceptedException == throwing.getClass()) {
			runnable.run();
			return true;
		}
		return false;
	}

	public static boolean ifResponseStatusExceptionWithStatusOrElseLog(Exception e, HttpStatus status, Runnable runnable) {
		if (e.getClass() == ResponseStatusException.class &&
				((ResponseStatusException) e).getStatusCode() == HttpStatusCode.valueOf(status.value())) {
			runnable.run();
			return true;
		}
		return false;
	}

	private static boolean isValidImageFormat(MultipartFile image) {
		String imageFormat;
		try {
			String originalFilename = image.getOriginalFilename();
			if (originalFilename == null) throw new ResponseStatusException(BAD_REQUEST);

			imageFormat = Files.probeContentType(Path.of(originalFilename));
		} catch (IOException e) {
			log.warn("Произошла ошибка при получение формата изображения", e);
			throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "", e);
		}
		return validImageFormats.contains(imageFormat);
	}

	private static String getUniqueFilename(MultipartFile image) {
		return UUID.randomUUID() + "_" + image.getOriginalFilename();
	}

	private static String buildUrlForImageByMethodName(Class<?> controller, String methodName, String filename) {
		try {
			return MvcUriComponentsBuilder.fromMethodName(
					controller,
					methodName,
					filename
			).toUriString();
		} catch (IllegalArgumentException e) {
			log.warn("Не удалось найти метод у %s с именем %s".formatted(PostController.class, methodName), e);
			throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "", e);
		}
	}

	@PostConstruct
	public void init() {
		postRepo = _postRepo;
		commentRepo = _commentRepo;
		userRepo = _userRepo;
	}
}