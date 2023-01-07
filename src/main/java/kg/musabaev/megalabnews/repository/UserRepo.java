package kg.musabaev.megalabnews.repository;

import kg.musabaev.megalabnews.model.User;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.repository.projection.UserItemView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {

	@Query(value = "INSERT INTO favourite_posts_users (user_id, post_id) VALUES (:userId, :postId)", nativeQuery = true)
	@Modifying
	void insertIntoFavouritePosts(@Param("userId") Long userId, @Param("postId") Long postId);

	@Query(value = "DELETE FROM favourite_posts_users WHERE post_id = :postId AND user_id = :userId", nativeQuery = true)
	@Modifying
	void deleteFromFavouritePosts(@Param("userId") Long userId, @Param("postId") Long postId);

	@Query(value = """
			SELECT
			    p.post_id AS id,
			    p.title AS title,
			    p.description AS description,
			    p.created_date AS createdDate,
			    p.image_url AS imageUrl
			FROM posts p
			JOIN favourite_posts_users fpu
			ON fpu.post_id = p.post_id AND fpu.user_id = :userId
			""", nativeQuery = true)
	Page<PostListView> findFavouritePostsByUserId(@Param("userId") Long userId, Pageable pageable);

	@Query("SELECT u.userPictureUrl FROM User u WHERE u.id = :userId")
	String findUserPictureByUserId(@Param("userId") Long userId);

	Optional<UserItemView> findProjectedById(Long userId);
}
