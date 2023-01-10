package kg.musabaev.megalabnews.repository;

import kg.musabaev.megalabnews.model.Comment;
import kg.musabaev.megalabnews.repository.projection.CommentListView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepo extends JpaRepository<Comment, Long> {

	Page<CommentListView> findRootsByPostIdAndParentIsNull(Long postId, Pageable pageable);

	Page<CommentListView> findChildrenByParentIdAndPostId(Long parentId, Long postId, Pageable pageable);

	boolean existsByIdAndPostId(Long commentId, Long postId);

	Optional<Comment> findByIdAndPostId(Long commentId, Long postId);

	@Query("SELECT c.id FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL")
	List<Long> getAllRootCommentId(@Param("postId") Long postId);

	@Query("SELECT c.id FROM Comment c WHERE c.parent.id = :parentId AND c.post.id = :postId")
	List<Long> getAllChildCommentIdByParentId(@Param("postId") Long postId, @Param("parentId") Long parentId);

	@Query(value = """
			SELECT u.username
			FROM comments c
			LEFT JOIN users u ON c.author_id = u.user_id
			WHERE c.post_id = :postId AND c.comment_id = :commentId""", nativeQuery = true)
	String findAuthorUsernameByPostIdAndCommentId(@Param("postId") Long postId, @Param("parentId") Long commentId);
}
