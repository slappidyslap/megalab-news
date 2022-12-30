package kg.musabaev.megalabnews.repository;

import kg.musabaev.megalabnews.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepo extends JpaRepository<Comment, Long> {

	/*@Query(value = """
	SELECT cast(count(*) AS bool) FROM comments c WHERE :commentId IN (
		SELECT c.comment_id WHERE post_id = :postId
	)""", nativeQuery = true)
	boolean existsByIdInPostByPostId(@Param("commentId") Long commentId, @Param("postId") Long postId);*/

	boolean existsByIdAndPostId(Long commentId, Long postId);

	Optional<Comment> findByIdAndPostId(Long commentId, Long postId);
}
