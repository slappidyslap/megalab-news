package kg.musabaev.megalabnews.dto;

public record AuthenticateRequest(
		String username,
		String password
) {
}
