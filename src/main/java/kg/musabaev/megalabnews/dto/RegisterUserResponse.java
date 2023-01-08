package kg.musabaev.megalabnews.dto;

import kg.musabaev.megalabnews.security.Authority;

import java.util.Set;

public record RegisterUserResponse(
		Long id,
		String name,
		String surname,
		String username,
		Set<Authority> authorities
) {
}
