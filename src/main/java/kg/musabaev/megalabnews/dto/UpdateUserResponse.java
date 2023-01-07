package kg.musabaev.megalabnews.dto;

public record UpdateUserResponse(
		Long id,
		String name,
		String surname,
		String username,
		String userPictureUrl
) {
}
