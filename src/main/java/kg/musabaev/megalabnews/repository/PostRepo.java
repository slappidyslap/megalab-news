package kg.musabaev.megalabnews.repository;

import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PostRepo extends JpaRepository<Post, Long> {

	Page<PostListView> findAllProjectedBy(Pageable pageable);

	Page<PostListView> findAllByTagsIn(Set<String> tags, Pageable pageable);

	boolean existsByTitle(String title);

	@Query(value = "SELECT tag FROM posts_tags WHERE post_id = :postId", nativeQuery = true)
	Set<String> findTagsByPostId(@Param("postId") Long postId);

	@Query("SELECT p.imageUrl FROM Post p WHERE p.id = :postId")
	String findPostImageUrlByPostId(@Param("postId") Long postId);

	@Query("SELECT p.id FROM Post p WHERE p.author.id = :author_id")
	List<Long> findAllPostsIdByAuthorId(@Param("author_id") Long authorId);
}
