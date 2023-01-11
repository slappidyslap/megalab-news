package kg.musabaev.megalabnews.exception;

public class PostNotFoundException extends RuntimeException {
	public PostNotFoundException() {
		super("post not found");
	}
	public PostNotFoundException(Throwable cause) {
		super("post not found", cause);
	}
}
