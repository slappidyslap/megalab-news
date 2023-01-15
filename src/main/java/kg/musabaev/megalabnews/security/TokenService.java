package kg.musabaev.megalabnews.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import kg.musabaev.megalabnews.exception.UserNotFoundException;
import kg.musabaev.megalabnews.repository.RefreshTokenRepo;
import kg.musabaev.megalabnews.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class TokenService {

	private final RefreshTokenRepo refreshTokenRepo;
	private final UserRepo userRepo;

	@Value("${app.security.access-token-expiration-ms}")
	private Long accessTokenExpirationMs;
	@Value("${app.security.refresh-token-expiration-ms}")
	private Long refreshTokenExpirationMs;
	@Value("${app.security.secret-key}")
	private char[] secretKeyAsCharArray;

	private Key secretKey;

	@PostConstruct
	private void init() {
		secretKey = Keys.hmacShaKeyFor(StandardCharsets.UTF_8.encode(CharBuffer.wrap(secretKeyAsCharArray)).array());
	}

	public String generateAccessToken(String username) {
		return Jwts.builder()
				.setSubject(username)
				.setIssuedAt(new Date())
				.setExpiration(Date.from(Instant.now().plusMillis(accessTokenExpirationMs)))
				.signWith(secretKey)
				.compact();
	}

	@Nullable
	public String getAccessTokenFromRequest(HttpServletRequest request) {
		String authorization = request.getHeader("Authorization");
		return authorization != null && authorization.startsWith("Bearer") ? authorization.substring(7) : null;
	}

	@Nullable
	public String getUsernameByAccessToken(String accessToken) {
		try {
			return Jwts.parserBuilder()
					.setSigningKey(secretKey)
					.build()
					.parseClaimsJws(accessToken)
					.getBody()
					.getSubject();
		} catch (Exception e) {
			return null;
		}
	}

	public String generateRefreshToken(Long userId) {
		assertUserExistsByIdOrElseThrow(userId);

		return refreshTokenRepo.findByOwnerId(userId).map(refreshToken -> {
			refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
			refreshToken.setToken(UUID.randomUUID().toString());

			return refreshTokenRepo.saveAndFlush(refreshToken).getToken();
		}).orElseGet(() -> refreshTokenRepo
				.save(new RefreshToken(
						userRepo.getReferenceById(userId),
						UUID.randomUUID().toString(),
						Instant.now().plusMillis(refreshTokenExpirationMs)))
				.getToken());
	}

	private void assertUserExistsByIdOrElseThrow(Long userId) {
		if (!userRepo.existsById(userId)) throw new UserNotFoundException();
	}
}
