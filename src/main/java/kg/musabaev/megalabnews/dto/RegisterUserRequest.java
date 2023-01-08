package kg.musabaev.megalabnews.dto;

public record RegisterUserRequest(
		String name,
		String surname,
		String username,
		String password
) {
}
