package kg.musabaev.megalabnews.repository;

import kg.musabaev.megalabnews.model.Comment;
import kg.musabaev.megalabnews.repository.projection.CommentListView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepo extends JpaRepository<Comment, Long> {

	Page<CommentListView> findRootsByPostIdAndParentIsNull(Long postId, Pageable pageable);

	Page<CommentListView> findChildrenByParentIdAndPostId(Long parentId, Long postId, Pageable pageable);

	boolean existsByIdAndPostId(Long commentId, Long postId);

	Optional<Comment> findByIdAndPostId(Long commentId, Long postId);
}
