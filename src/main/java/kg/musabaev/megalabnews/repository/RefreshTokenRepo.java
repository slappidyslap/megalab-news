package kg.musabaev.megalabnews.repository;

import kg.musabaev.megalabnews.security.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByOwnerId(Long userId);

	@EntityGraph(attributePaths = "owner")
	Optional<RefreshToken> findByToken(String token);

	void deleteByOwnerId(Long ownerId);
}
