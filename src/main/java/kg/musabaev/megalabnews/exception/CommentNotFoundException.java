package kg.musabaev.megalabnews.exception;

public class CommentNotFoundException extends RuntimeException {
	public CommentNotFoundException() {
		super("comment not found");
	}
	public CommentNotFoundException(Throwable cause) {
		super("comment not found", cause);
	}
}
