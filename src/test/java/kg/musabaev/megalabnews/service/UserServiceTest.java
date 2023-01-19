package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.AddToFavouritePostsRequest;
import kg.musabaev.megalabnews.dto.UpdateUserRequest;
import kg.musabaev.megalabnews.exception.PostNotFoundException;
import kg.musabaev.megalabnews.exception.ResponseStatusConflictException;
import kg.musabaev.megalabnews.exception.UserNotFoundException;
import kg.musabaev.megalabnews.mapper.UserMapper;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.model.User;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.RefreshTokenRepo;
import kg.musabaev.megalabnews.repository.UserRepo;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.repository.projection.UserItemView;
import kg.musabaev.megalabnews.service.impl.SimpleUserService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserServiceTest {

	@Mock
	UserRepo userRepo;
	@Mock
	PostRepo postRepo;
	@Mock
	RefreshTokenRepo refreshTokenRepo;
	@Spy
	UserMapper userMapper = Mappers.getMapper(UserMapper.class);

	@InjectMocks
	SimpleUserService service;

	final SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

	@Test
	void shouldReturnUser() {
		Long userId = 23L;
		UserItemView exceptedUser = projectionFactory.createProjection(UserItemView.class, User.builder()
				.id(userId)
				.name("Урмат")
				.surname("Жумабеков")
				.username("urmat")
				.build());

		when(userRepo.findProjectedById(userId)).thenReturn(Optional.of(exceptedUser));

		UserItemView actualUser = service.getById(userId);

		assertThat(exceptedUser)
				.usingRecursiveComparison()
				.isEqualTo(actualUser);
	}

	@Test
	void shouldThrowUserNotFound_whenReceivingUser() {
		Long userId = 233L;

		when(userRepo.findProjectedById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(userId))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void shouldAddToFavouritePosts() {
		Long userId = 1232L;
		var addToFavPostsReq = new AddToFavouritePostsRequest(123L);

		when(userRepo.existsById(userId)).thenReturn(true);
		when(postRepo.existsById(addToFavPostsReq.postId())).thenReturn(true);

		assertThatCode(() -> service.addToFavouritePosts(userId, addToFavPostsReq))
				.doesNotThrowAnyException();

		verify(userRepo, times(1)).insertIntoFavouritePosts(userId, addToFavPostsReq.postId());
	}

	@Test
	void shouldThrowUserNotFound_whenAddingToFavPosts() {
		Long userId = 1232L;
		var addToFavPostsReq = new AddToFavouritePostsRequest(123L);

		when(userRepo.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> service.addToFavouritePosts(userId, addToFavPostsReq))
				.isInstanceOf(UserNotFoundException.class);

		verify(userRepo, never()).insertIntoFavouritePosts(userId, addToFavPostsReq.postId());
	}

	@Test
	void shouldThrowPostNotFound_whenAddingToFavPosts() {
		Long userId = 1232L;
		var addToFavPostsReq = new AddToFavouritePostsRequest(123L);

		when(userRepo.existsById(userId)).thenReturn(true);
		when(postRepo.existsById(addToFavPostsReq.postId())).thenReturn(false);


		assertThatThrownBy(() -> service.addToFavouritePosts(userId, addToFavPostsReq))
				.isInstanceOf(PostNotFoundException.class);

		verify(userRepo, never()).insertIntoFavouritePosts(userId, addToFavPostsReq.postId());
	}

	@Test
	void shouldThrowConflictNotFound_whenAddingToFavPosts() {
		Long userId = 1232L;
		var addToFavPostsReq = new AddToFavouritePostsRequest(123L);

		when(userRepo.existsById(userId)).thenReturn(true);
		when(postRepo.existsById(addToFavPostsReq.postId())).thenReturn(true);
		doThrow(DataIntegrityViolationException.class)
				.when(userRepo)
				.insertIntoFavouritePosts(userId, addToFavPostsReq.postId());

		assertThatThrownBy(() -> service.addToFavouritePosts(userId, addToFavPostsReq))
				.isInstanceOf(ResponseStatusConflictException.class);

		verify(userRepo, times(1)).insertIntoFavouritePosts(userId, addToFavPostsReq.postId()); // чекай сервис
	}


	@Test
	void shouldDeleteFromFavouritePosts() {
		Long userId = 123L;
		Long postId = 1223L;

		when(userRepo.existsById(userId)).thenReturn(true);

		assertThatCode(() -> service.deleteFromFavouritePosts(userId, postId))
				.doesNotThrowAnyException();

		verify(userRepo, times(1)).deleteFromFavouritePosts(userId, postId);
	}

	@Test
	void shouldThrowUserNotFound_whenDeletingFromFavPosts() {
		Long userId = 2312L;
		Long postId = 123L;

		when(userRepo.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> service.deleteFromFavouritePosts(userId, postId))
				.isInstanceOf(UserNotFoundException.class);

		verify(userRepo, never()).deleteFromFavouritePosts(userId, postId);
	}

	@Test
	void shouldReturnAllFavouritePosts() {
		Long userId = 123L;

		when(userRepo.existsById(userId)).thenReturn(true);
		when(userRepo.findFavouritePostsByUserId(userId, Pageable.ofSize(10))).thenReturn(getPosts());

		var favouritePosts = service.getAllFavouritePostsByUserId(userId, Pageable.ofSize(10));

		assertThat(favouritePosts.getContent()).hasSize(4);

		verify(userRepo, times(1)).findFavouritePostsByUserId(userId, Pageable.ofSize(10));
	}

	@Test
	void shouldThrowUserNotFound_whenReceivingFavPosts() {
		Long userId = 243L;

		when(userRepo.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> service.getAllFavouritePostsByUserId(userId, any()))
				.isInstanceOf(UserNotFoundException.class);

		verify(userRepo, never()).findFavouritePostsByUserId(userId, Pageable.ofSize(10));

	}

	@Test
	void shouldReturnAllCreatedPosts() {
		Long userId = 123L;

		when(userRepo.existsById(userId)).thenReturn(true);
		when(postRepo.findAllByAuthorId(userId, Pageable.ofSize(10))).thenReturn(getPosts());

		var createdPosts = service.getAllCreatedPostsByUserId(userId, Pageable.ofSize(10));

		assertThat(createdPosts.getContent()).hasSize(4);

		verify(postRepo, times(1)).findAllByAuthorId(userId, Pageable.ofSize(10));
	}

	@Test
	void shouldThrowUserNotFound_whenReceivingCreatedPosts() {
		Long userId = 243L;

		when(userRepo.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> service.getAllCreatedPostsByUserId(userId, any()))
				.isInstanceOf(UserNotFoundException.class);

		verify(postRepo, never()).findAllByAuthorId(userId, Pageable.ofSize(10));
	}

	@Test
	void shouldUpdateUser() {
		Long userId = 123L;
		var updateUserRequest = getUpdateUserRequest();

		when(userRepo.existsByUsername(updateUserRequest.username())).thenReturn(false);
		when(userRepo.findById(userId)).thenReturn(Optional.of(new User()));
		when(userRepo.save(any(User.class))).then(i -> i.getArgument(0));

		var updateUserResponse = service.update(userId, updateUserRequest);

		assertThat(updateUserRequest)
				.usingRecursiveComparison()
				.ignoringFields("id")
				.isEqualTo(updateUserResponse);

		verify(userRepo, times(1)).save(any());
	}

	@Test
	void shouldThrowUserExists_whenUpdatingUser() {
		Long userId = 123L;
		var updateUserRequest = getUpdateUserRequest();

		when(userRepo.existsByUsername(updateUserRequest.username())).thenReturn(true);

		assertThatThrownBy(() -> service.update(userId, updateUserRequest))
				.isInstanceOf(ResponseStatusConflictException.class);

		verify(userRepo, never()).save(any());
		verify(userRepo, never()).findById(userId);
	}

	@Test
	void shouldThrowUserNotFound_whenUpdatingUser() {
		Long userId = 123L;
		var updateUserRequest = getUpdateUserRequest();

		when(userRepo.existsByUsername(updateUserRequest.username())).thenReturn(false);
		when(userRepo.findById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(userId, updateUserRequest))
				.isInstanceOf(UserNotFoundException.class);

		verify(userRepo, never()).save(any());
	}

	@Test
	void shouldDeleteUser() {
		Long userId = 123L;

		when(userRepo.existsById(userId)).thenReturn(true);

		assertThatCode(() -> service.deleteById(userId))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldThrowUserNotFound_whenDeletingUser() {
		Long userId = 1231L;

		when(userRepo.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> service.deleteById(userId))
				.isInstanceOf(UserNotFoundException.class);
	}

	private UpdateUserRequest getUpdateUserRequest() {
		return new UpdateUserRequest(
				"new name",
				"new surname",
				"slappidyslap",
				null);
	}

	private Page<PostListView> getPosts() {
		return new PageImpl<>(List.of(
				createPostListViewBy(Post.builder().id(1L).build()),
				createPostListViewBy(Post.builder().id(2L).build()),
				createPostListViewBy(Post.builder().id(3L).build()),
				createPostListViewBy(Post.builder().id(4L).build())
		));
	}

	private PostListView createPostListViewBy(Post post) {
		return projectionFactory.createProjection(PostListView.class, post);
	}
}