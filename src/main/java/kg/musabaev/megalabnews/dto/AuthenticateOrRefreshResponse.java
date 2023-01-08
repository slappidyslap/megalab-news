package kg.musabaev.megalabnews.dto;

import kg.musabaev.megalabnews.security.Authority;

import java.util.Set;

public record AuthenticateOrRefreshResponse(
		String accessToken,
		String refreshToken,
		UserInfo user
) {

	public record UserInfo(
			Long id,
			String name,
			String surname,
			String username,
			Set<Authority> authorities,
			String userPictureUrl
	) {
	}
}
