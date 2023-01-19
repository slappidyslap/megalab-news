package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.UpdateCommentRequest;
import kg.musabaev.megalabnews.exception.CommentNotFoundException;
import kg.musabaev.megalabnews.exception.PostNotFoundException;
import kg.musabaev.megalabnews.mapper.CommentMapper;
import kg.musabaev.megalabnews.model.Comment;
import kg.musabaev.megalabnews.repository.CommentRepo;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.projection.CommentListView;
import kg.musabaev.megalabnews.service.impl.SimpleCommentService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class CommentServiceTest {

	@Mock
	CommentRepo commentRepo;
	@Mock
	PostRepo postRepo;
	@Spy
	CommentMapper commentMapper = Mappers.getMapper(CommentMapper.class);

	@InjectMocks
	SimpleCommentService service;

	final SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

	@Test
	void shouldSaveRootComment() {
		Long postId = 321L;
		var newCommentRequest = getNewCommentRequest(null);

		when(postRepo.existsById(postId)).thenReturn(true);
		when(commentRepo.save(any(Comment.class))).then(i -> i.getArgument(0));

		var newCommentResponse = service.save(postId, newCommentRequest);

		assertThat(newCommentRequest)
				.usingRecursiveComparison()
				.comparingOnlyFields("parentId", "content")
				.isEqualTo(newCommentResponse);

		verify(commentRepo, times(1)).save(any());
	}

	@Test
	void shouldThrowPostNotFound_whenSavingRootComment() {
		Long postId = 321L;

		when(postRepo.existsById(postId)).thenReturn(false);

		assertThatThrownBy(() -> service.save(postId, any()))
				.isInstanceOf(PostNotFoundException.class);

		verify(commentRepo, never()).save(any());
	}

	@Test
	void shouldSaveChildComment() {
		Long postId = 321L;
		Long parentCommentId = 34L;
		Comment parentComment = Comment.builder().id(parentCommentId).build();
		var newCommentRequest = getNewCommentRequest(parentCommentId);

		when(postRepo.existsById(postId)).thenReturn(true);
		when(commentRepo.existsByIdAndPostId(parentCommentId, postId)).thenReturn(true);
		when(commentRepo.getReferenceById(parentCommentId)).thenReturn(parentComment);
		when(commentRepo.save(any(Comment.class))).then(i -> i.getArgument(0));

		var newCommentResponse = service.save(postId, newCommentRequest);

		assertThat(newCommentRequest)
				.usingRecursiveComparison()
				.comparingOnlyFields("parentId", "content")
				.isEqualTo(newCommentResponse);

		verify(commentRepo, times(1)).save(any());
	}

	@Test
	void shouldThrowCommentNotFound_whenSavingChildComment() {
		Long postId = 321L;
		Long parentCommentId = 432L;
		var newCommentRequest = getNewCommentRequest(parentCommentId);

		when(postRepo.existsById(postId)).thenReturn(true);
		when(commentRepo.existsByIdAndPostId(parentCommentId, postId)).thenReturn(false);

		assertThatThrownBy(() -> service.save(postId, newCommentRequest))
				.isInstanceOf(CommentNotFoundException.class);

		verify(commentRepo, never()).save(any());
	}

	@Test
	void shouldReturnRootComments() {
		Long postId = 324L;

		when(postRepo.existsById(postId)).thenReturn(true);
		when(commentRepo.findRootsByPostIdAndParentIsNull(postId, Pageable.ofSize(10)))
				.thenReturn(getComments());

		Page<CommentListView> rootComments = service.getRootsByPostId(postId, Pageable.ofSize(10));

		assertThat(rootComments.getContent()).hasSize(3);
	}

	@Test
	void shouldThrowPostNotFound_whenReceivingRootComments() {
		Long postId = 324L;

		when(postRepo.existsById(postId)).thenReturn(false);

		assertThatThrownBy(() -> service.getRootsByPostId(postId, Pageable.ofSize(10)))
				.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	void shouldReturnChildComments() {
		Long postId = 321L;
		Long parentCommentId = 32L;

		when(postRepo.existsById(postId)).thenReturn(true);
		when(commentRepo.existsByIdAndPostId(parentCommentId, postId)).thenReturn(true);
		when(commentRepo.findChildrenByParentIdAndPostId(parentCommentId, postId, Pageable.ofSize(10)))
				.thenReturn(getComments());

		Page<CommentListView> childComments =
				service.getChildrenByParentId(postId, parentCommentId, Pageable.ofSize(10));

		assertThat(childComments.getContent()).hasSize(3);
	}

	@Test
	void shouldThrowPostNotFound_whenReceivingChildComments() {
		Long postId = 324L;

		when(postRepo.existsById(postId)).thenReturn(false);

		assertThatThrownBy(() -> service.getChildrenByParentId(postId, anyLong(), Pageable.ofSize(10)))
				.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	void shouldThrowCommentNotFound_whenReceivingChildComments() {
		Long postId = 324L;
		Long parentCommentId = 312L;

		when(postRepo.existsById(postId)).thenReturn(true);
		when(commentRepo.existsByIdAndPostId(parentCommentId, postId)).thenReturn(false);

		assertThatThrownBy(() -> service.getChildrenByParentId(postId, parentCommentId, Pageable.ofSize(10)))
				.isInstanceOf(CommentNotFoundException.class);

		verify(commentRepo, never()).findChildrenByParentIdAndPostId(anyLong(), anyLong(), any());
	}

	@Test
	void shouldUpdateComment() {
		Long postId = 312L;
		Long commentId = 12L;
		var updateCommentRequest = new UpdateCommentRequest("new content");

		when(postRepo.existsById(postId)).thenReturn(true);
		when(commentRepo.findByIdAndPostId(commentId, postId)).thenReturn(Optional.of(new Comment()));
		when(commentRepo.save(any(Comment.class))).then(i -> i.getArgument(0));

		var updateCommentResponse = service.update(postId, commentId, updateCommentRequest);

		assertThat(updateCommentRequest.content()).isEqualTo(updateCommentResponse.content());

		verify(commentRepo, times(1)).save(any());
	}

	@Test
	void shouldThrowPostNotFound_whenUpdatingComment() {
		Long postId = 12L;
		Long commentId = 31L;

		when(postRepo.existsById(postId)).thenReturn(false);

		assertThatThrownBy(() -> service.update(postId, commentId, any()))
				.isInstanceOf(PostNotFoundException.class);

		verify(commentRepo, never()).save(any());
	}

	@Test
	void shouldThrowCommentNotFound_whenUpdatingComment() {
		Long postId = 321L;
		Long commentId = 12L;

		when(postRepo.existsById(postId)).thenReturn(true);
		when(commentRepo.findByIdAndPostId(commentId, postId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(postId, commentId, any()))
				.isInstanceOf(CommentNotFoundException.class);

		verify(commentRepo, never()).save(any());
	}

	@Test
	void shouldDeleteComment() {
		Long postId = 12L;
		Long commentId = 1234L;

		when(commentRepo.existsByIdAndPostId(commentId, postId))
				.thenReturn(true);

		assertThatCode(() -> service.deleteById(postId, commentId))
				.doesNotThrowAnyException();

		verify(commentRepo, times(1)).deleteById(commentId);
	}

	@Test
	void shouldThrowCommentNotFound_whenDeletingComment() {
		Long postId = 12L;
		Long commentId = 1234L;

		when(commentRepo.existsByIdAndPostId(commentId, postId))
				.thenReturn(false);

		assertThatThrownBy(() -> service.deleteById(postId, commentId))
				.isInstanceOf(CommentNotFoundException.class);

		verify(commentRepo, never()).deleteById(commentId);
	}

	private NewCommentRequest getNewCommentRequest(Long parentId) {
		return new NewCommentRequest(parentId, "content");
	}

	private Page<CommentListView> getComments() {
		return new PageImpl<>(List.of(
				createCommentListViewBy(Comment.builder().id(2L).content("rtx").build()),
				createCommentListViewBy(Comment.builder().id(3L).content("4090").build()),
				createCommentListViewBy(Comment.builder().id(1L).content("super").build())
		));
	}

	private CommentListView createCommentListViewBy(Comment source) {
		return projectionFactory.createProjection(CommentListView.class, source);
	}
}