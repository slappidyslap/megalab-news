package kg.musabaev.megalabnews.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class RestApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	void handleMaxUploadSizeExceededException(HttpServletResponse response) {
		response.setStatus(HttpStatus.BAD_REQUEST.value());
	}

	/**
	 * PropertyReferenceException выбрасывается,
	 * когда мы Page параметризованный неким типом ({@link org.springframework.data.domain.Page})
	 * сортируем по имени поля, которого не существует в этом типе
	 */
	@ExceptionHandler(PropertyReferenceException.class)
	void handlePropertyReferenceException(HttpServletResponse response) {
		response.setStatus(HttpStatus.BAD_REQUEST.value());
	}

	@ExceptionHandler(ResponseStatusConflictException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	void setConflictStatusCode() {
	}

	@ExceptionHandler(ResponseStatusBadRequestException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	void setBadRequestStatusCode() {
	}

	@ExceptionHandler(ResponseStatusUnauthorizedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	void setUnauthorizedStatusCode() {
	}

	@ExceptionHandler(ResponseStatusInternalServerErrorException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	void setInternalServerErrorStatusCode() {
	}

	@ExceptionHandler(ResponseStatusForbiddenException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	void setForbiddenStatusCode() {
	}

	@ExceptionHandler({
			UserNotFoundException.class, PostNotFoundException.class,
			CommentNotFoundException.class, ResponseStatusNotFoundException.class})
	@ResponseStatus(NOT_FOUND)
	Map<String, Boolean> setNotFoundStatusCode(Exception e) {
		return Map.of(
				"isPostNotFound", e.getClass() == PostNotFoundException.class,
				"isCommentNotFound", e.getClass() == CommentNotFoundException.class,
				"isUserNotFound", e.getClass() == UserNotFoundException.class,
				"isAnotherResourceNotFound", e.getClass() == ResponseStatusNotFoundException.class
		);
	}

	@Override
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		return super.handleMethodArgumentNotValid(ex, headers, status, request);
	}
}
