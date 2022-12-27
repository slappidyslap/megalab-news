package kg.musabaev.megalabnews.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class RestApiExceptionHandler {

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	void handleMaxUploadSizeExceededException(HttpServletResponse response) {
		response.setStatus(HttpStatus.BAD_REQUEST.value());
	}


	/**
	 PropertyReferenceException выбрасывается,
	 когда мы Page параметризованный неким типом ({@link org.springframework.data.domain.Page})
	 сортируем по имени поля, которого не существует в типе
	 */
	@ExceptionHandler(PropertyReferenceException.class)
	void handlePropertyReferenceException(HttpServletResponse response) {
		response.setStatus(HttpStatus.BAD_REQUEST.value());
	}
}
