package kg.musabaev.megalabnews.util;

import kg.musabaev.megalabnews.controller.PostController;
import kg.musabaev.megalabnews.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

	public static Resource getUploadedFileByFilenameInStorage(String filename, Path storage) {
		try {
			var image = new UrlResource(storage.resolve(filename).toUri());

			if (!image.exists() || !image.isReadable()) {
				throw new ResponseStatusNotFoundException();
			}
			return image;
		} catch (MalformedURLException e) {
			throw new ResponseStatusInternalServerErrorException(e);
		}
	}

	public static String uploadFileAndGetUrlFromMethodName(
			MultipartFile file,
			Path storage,
			Class<?> controller,
			String methodName) {
		if (!isValidImageFormat(file)) throw new ResponseStatusBadRequestException();
		String uniqueFilename = getUniqueFilename(file);
		Path pathToSave = storage.resolve(uniqueFilename);
		String fileUrl = buildUrlForImageByMethodName(controller, methodName, uniqueFilename);
		try {
			file.transferTo(pathToSave);
		} catch (IOException e) {
			throw new ResponseStatusInternalServerErrorException(e);
		}
		return fileUrl;
	}

	public static MediaType getMediaTypeByFilename(String filename) {
		return MediaType.parseMediaType(MimeMappings.DEFAULT.get(filename.substring(filename.lastIndexOf("."))));
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

	public static boolean isAuthenticatedUser(String actualUserUsername) {
		UserDetails authenticatedPrincipal = (UserDetails) SecurityContextHolder
				.getContext()
				.getAuthentication()
				.getPrincipal();
		return authenticatedPrincipal.getUsername().equals(actualUserUsername);
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

	public static boolean ifUserNotFound(Exception e, Runnable runnable) {
		return ifExceptionEqualsOrElseLog(e, UserNotFoundException.class, runnable);
	}

	public static boolean ifInternalServerError(Exception e, Runnable runnable) {
		return ifResponseStatusExceptionWithStatusOrElseLog(e, INTERNAL_SERVER_ERROR, runnable);
	}

	public static boolean ifConflict(Exception e, Runnable runnable) {
		return ifResponseStatusExceptionWithStatusOrElseLog(e, CONFLICT, runnable);
	}

	public static boolean ifBadRequest(Exception e, Runnable runnable) {
		return ifResponseStatusExceptionWithStatusOrElseLog(e, BAD_REQUEST, runnable);
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
			if (originalFilename == null) throw new ResponseStatusBadRequestException();

			imageFormat = Files.probeContentType(Path.of(originalFilename));
		} catch (IOException e) {
			log.warn("Произошла ошибка при получение формата изображения", e);
			throw new ResponseStatusInternalServerErrorException(e);
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
			throw new ResponseStatusInternalServerErrorException(e);
		}
	}
}