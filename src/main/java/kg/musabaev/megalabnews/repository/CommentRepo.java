package kg.musabaev.megalabnews.repository;

import kg.musabaev.megalabnews.model.Comment;
import kg.musabaev.megalabnews.repository.projection.NonRootCommentListView;
import kg.musabaev.megalabnews.repository.projection.RootCommentListView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepo extends JpaRepository<Comment, Long> {

	Page<RootCommentListView> findRootsByPostIdAndParentIsNull(Long postId, Pageable pageable);

	Page<NonRootCommentListView> findChildrenByParentIdAndPostId(Long parentId, Long postId, Pageable pageable);

	boolean existsByIdAndPostId(Long commentId, Long postId);

	Optional<Comment> findByIdAndPostId(Long commentId, Long postId);
}
