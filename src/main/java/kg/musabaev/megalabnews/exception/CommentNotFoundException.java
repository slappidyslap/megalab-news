package kg.musabaev.megalabnews.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CommentNotFoundException extends RuntimeException {
	public CommentNotFoundException() {
		super("comment not found");
	}
	public CommentNotFoundException(Throwable cause) {
		super("comment not found", cause);
	}
}
