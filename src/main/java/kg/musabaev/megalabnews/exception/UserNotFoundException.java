package kg.musabaev.megalabnews.exception;

public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException() {
		super("user not found");
	}
	public UserNotFoundException(Throwable cause) {
		super("user not found", cause);
	}
}
