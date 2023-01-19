package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.exception.PostNotFoundException;
import kg.musabaev.megalabnews.exception.ResponseStatusConflictException;
import kg.musabaev.megalabnews.mapper.PostMapper;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.CommentRepo;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.UserRepo;
import kg.musabaev.megalabnews.repository.projection.PostItemView;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.service.impl.SimplePostService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class PostServiceTest {

	@Mock
	PostRepo postRepo;
	@Mock
	CommentRepo commentRepo;
	@Mock
	UserRepo userRepo;
	@Spy
	PostMapper postMapper = Mappers.getMapper(PostMapper.class);

	@InjectMocks
	SimplePostService service;

	final SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();


	@Test
	void shouldSavePost() {
		String title = "title";

		when(postRepo.existsByTitle(title)).thenReturn(false);
		when(postMapper.toModel(any())).thenCallRealMethod();
		when(postMapper.toDto(any())).thenCallRealMethod();
		when(postRepo.save(any(Post.class))).then(i -> i.getArgument(0));

		NewOrUpdatePostRequest newPostRequest = getNewOrUpdatePostRequest(title);

		var newPostResponse = service.save(newPostRequest);

		assertThat(newPostRequest)
				.usingRecursiveComparison()
				.comparingOnlyFields("title", "description", "content", "tags", "imageUrl")
				.isEqualTo(newPostResponse);

		verify(postRepo, times(1)).save(any(Post.class));
	}


	@Test
	void shouldThrowPostExists_whenSavingPost() {
		String title = "title";

		when(postRepo.existsByTitle(title)).thenReturn(true);

		assertThatThrownBy(() -> service.save(getNewOrUpdatePostRequest(title)))
				.isInstanceOf(ResponseStatusConflictException.class);

		verify(postRepo, times(0)).save(any(Post.class));
	}

	@Test
	void shouldReturnPostsByTag() {
		when(postRepo.findAllByTagsIn(Set.of("Спорт"), PageRequest.ofSize(10)))
				.thenReturn(getFilteredPosts());

		Page<PostListView> filteredPosts = service.getAll(PageRequest.ofSize(10), Set.of("Спорт"));

		assertThat(filteredPosts.getContent()).hasSize(2);
	}

	@Test
	void shouldReturnAllPost() {
		when(postRepo.findAllProjectedBy(PageRequest.ofSize(10)))
				.thenReturn(getPosts());

		Page<PostListView> posts = service.getAll(PageRequest.ofSize(10), null);

		assertThat(posts.getContent()).hasSize(4);
	}

	@Test
	void shouldReturnPostById() {
		PostItemView exceptedPost = projectionFactory.createProjection(PostItemView.class, Post.builder()
				.id(3L)
				.title("hello3")
				.build());
		when(postRepo.findProjectedById(3L))
				.thenReturn(Optional.of(exceptedPost));

		PostItemView actualPost = service.getById(3L);

		assertThat(actualPost.getTitle()).isEqualTo(exceptedPost.getTitle());
	}

	@Test
	void shouldThrowPostNotFound() {
		when(postRepo.findProjectedById(5L))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(5L))
				.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	void shouldDeletePost() {
		Long id = 3L;
		when(postRepo.existsById(id)).thenReturn(true);

		assertThatCode(() -> service.deleteById(id)).doesNotThrowAnyException();

		verify(postRepo, times(1)).deleteById(id);
	}

	@Test
	void shouldThrowPostNotFound_whenDeletingPost() {
		Long id = 3L;
		when(postRepo.existsById(id)).thenReturn(false);

		assertThatCode(() -> service.deleteById(id))
				.isInstanceOf(PostNotFoundException.class);

		verify(postRepo, times(0)).deleteById(id);
	}

	@Test
	void shouldUpdatePost() {
		Long postId = 3L;
		String title = "new title";
		Post persistedPost = getOldPost();
		var updatePostRequest = getNewOrUpdatePostRequest(title);

		when(postRepo.findById(postId)).thenReturn(Optional.of(persistedPost));
		when(postRepo.existsByTitle(title)).thenReturn(false);
		when(postMapper.toDto(persistedPost)).thenCallRealMethod();
		when(postRepo.save(persistedPost)).then(i -> i.getArgument(0));

		var updatePostResponse = service.update(postId, updatePostRequest);

		assertThat(updatePostRequest)
				.usingRecursiveComparison()
				.comparingOnlyFields("title", "description", "content", "tags", "imageUrl")
				.isEqualTo(updatePostResponse);

		verify(postRepo, times(1)).save(any(Post.class));
	}

	@Test
	void shouldThrowPostNotFound_whenUpdatingPost() {
		Long postId = 3L;
		when(postRepo.findById(postId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(postId, any(NewOrUpdatePostRequest.class)))
				.isInstanceOf(PostNotFoundException.class);

		verify(postRepo, times(0)).save(any(Post.class));
	}

	@Test
	void shouldThrowPostExists_whenUpdatingPost() {
		Long postId = 3L;
		String title = "existing title";
		Post persistedPost = getOldPost();
		var updatePostRequest = getNewOrUpdatePostRequest(title);

		when(postRepo.findById(postId)).thenReturn(Optional.of(persistedPost));
		when(postRepo.existsByTitle(title)).thenReturn(true);

		assertThatThrownBy(() -> service.update(postId, updatePostRequest))
				.isInstanceOf(ResponseStatusConflictException.class);

		verify(postRepo, times(0)).save(any(Post.class));
	}

	private Post getOldPost() {
		return Post.builder()
				.id(3L)
				.title("old title")
				.description("old desc")
				.content("old content")
				.build();
	}

	private NewOrUpdatePostRequest getNewOrUpdatePostRequest(String title) {
		return NewOrUpdatePostRequest.builder()
				.title(title)
				.description("desc")
				.content("content")
				.tags(new LinkedHashSet<>())
				.build();
	}

	private Page<PostListView> getFilteredPosts() {
		return new PageImpl<>(List.of(
				createPostListViewBy(Post.builder().id(3L).title("hello3").tags(Set.of("Спорт", "Наука")).build()),
				createPostListViewBy(Post.builder().id(4L).title("hello4").tags(Set.of("Спорт")).build())
		));
	}

	private Page<PostListView> getPosts() {
		return new PageImpl<>(
				Stream.concat(
						Stream.of(
								createPostListViewBy(Post.builder().id(1L).title("hello3").tags(Set.of("Развлечение")).build()),
								createPostListViewBy(Post.builder().id(2L).title("hello4").tags(Set.of("Политика")).build())),
						getFilteredPosts().stream()
				).toList());
	}

	private PostListView createPostListViewBy(Post post) {
		return projectionFactory.createProjection(PostListView.class, post);
	}

}