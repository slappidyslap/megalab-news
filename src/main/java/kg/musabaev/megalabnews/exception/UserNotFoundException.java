package kg.musabaev.megalabnews.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException() {
		super("user not found");
	}
	public UserNotFoundException(Throwable cause) {
		super("user not found", cause);
	}
}
