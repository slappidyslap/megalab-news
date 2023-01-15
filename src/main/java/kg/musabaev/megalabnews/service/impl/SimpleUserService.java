package kg.musabaev.megalabnews.service.impl;

import jakarta.annotation.PostConstruct;
import kg.musabaev.megalabnews.controller.UserController;
import kg.musabaev.megalabnews.dto.AddToFavouritePostsRequest;
import kg.musabaev.megalabnews.dto.UpdateUserRequest;
import kg.musabaev.megalabnews.dto.UpdateUserResponse;
import kg.musabaev.megalabnews.exception.PostNotFoundException;
import kg.musabaev.megalabnews.exception.ResponseStatusConflictException;
import kg.musabaev.megalabnews.exception.UserNotFoundException;
import kg.musabaev.megalabnews.mapper.UserMapper;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.RefreshTokenRepo;
import kg.musabaev.megalabnews.repository.UserRepo;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.repository.projection.UserItemView;
import kg.musabaev.megalabnews.service.UserService;
import kg.musabaev.megalabnews.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Primary
@Log4j2
public class SimpleUserService implements UserService {

	public static final String USER_ITEM_CACHE_NAME = "userItem";
	public static final String USER_FAVOURITE_POSTS_CACHE_NAME = "userFavouritePosts";
	public static final String USER_CREATED_POSTS_CACHE_NAME = "userCreatedPosts";
	public static final String USER_PICTURE_CACHE_NAME = "userPicture";

	private final UserRepo userRepo;
	private final UserMapper userMapper;
	private final PostRepo postRepo;
	private final RefreshTokenRepo refreshTokenRepo;

	@Value("${app.storage.folder-name}")
	private String storageFolderName;
	@Value("${app.storage.user-picture-folder-name}")
	private String userPictureFolderName;
	private Path storage;

	@PostConstruct
	void setUp() {
		storage = Path.of(storageFolderName, userPictureFolderName);
	}

	@Override
	@Cacheable(USER_ITEM_CACHE_NAME)
	public UserItemView getById(Long userId) {
		return userRepo.findProjectedById(userId).orElseThrow(() -> {
			throw new UserNotFoundException();
		});
	}

	@Override
	@Transactional
	public void addToFavouritePosts(Long userId, AddToFavouritePostsRequest dto) {
		assertUserExistsByIdOrElseThrow(userId);
		assertPostExistsByIdOrElseThrow(dto.postId());

		try {
			userRepo.insertIntoFavouritePosts(userId, dto.postId());
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusConflictException();
		}
	}

	@Override
	@Transactional
	public void deleteFromFavouritePosts(Long userId, Long postId) {
		assertUserExistsByIdOrElseThrow(userId);

		userRepo.deleteFromFavouritePosts(userId, postId);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = USER_FAVOURITE_POSTS_CACHE_NAME, keyGenerator = "pairCacheKeyGenerator")
	public Page<PostListView> getAllFavouritePostsByUserId(Long userId, Pageable pageable) {
		assertUserExistsByIdOrElseThrow(userId);

		return userRepo.findFavouritePostsByUserId(userId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = USER_CREATED_POSTS_CACHE_NAME, keyGenerator = "pairCacheKeyGenerator")
	public Page<PostListView> getAllCreatedPostsByUserId(Long userId, Pageable pageable) {
		assertUserExistsByIdOrElseThrow(userId);

		return postRepo.findAllByAuthorId(userId, pageable);
	}

	@Override
	@Transactional
	@CacheEvict(cacheNames = USER_ITEM_CACHE_NAME, key = "#userId")
	public UpdateUserResponse update(Long userId, UpdateUserRequest dto) {
		if (userRepo.existsByUsername(dto.username())) throw new ResponseStatusConflictException();

		return userRepo.findById(userId).map(user -> {
			userMapper.update(dto, user);

			return userMapper.toUpdateUserDto(userRepo.save(user));
		}).orElseThrow(() -> {
			throw new UserNotFoundException();
		});
	}

	@Override
	@Transactional
	public Map<String, String> uploadUserPicture(MultipartFile userPicture) {
		return Map.of("userPicture", Utils.uploadFileAndGetUrlFromMethodName(
				userPicture,
				storage,
				UserController.class,
				"getUserPictureByFilename"));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(USER_PICTURE_CACHE_NAME)
	public Resource getUserPictureByFilename(String filename) {
		return Utils.getUploadedFileByFilenameInStorage(filename, storage);
	}

	@Override
	@Transactional
	@Caching(evict = {
			@CacheEvict(USER_ITEM_CACHE_NAME),
			@CacheEvict(cacheNames = USER_PICTURE_CACHE_NAME, allEntries = true)})
	public void deleteById(Long userId) {
		assertUserExistsByIdOrElseThrow(userId);

		postRepo.findAllPostsIdByAuthorId(userId).forEach(postRepo::deleteById);
		deleteImageInStorageIfExists(Utils.getLastPathSegmentOrNull(userRepo.findUserPictureByUserId(userId)));
		refreshTokenRepo.deleteByOwnerId(userId);

		userRepo.deleteById(userId);
	}

	private void deleteImageInStorageIfExists(String filename) {
		Utils.deleteFileFromStorageIfExists(
				filename,
				storage,
				() -> log.debug("Изображение пользователя с названием {} удален", filename),
				exception -> log.warn("Произошла ошибка при удалении изображения пользователя", exception));
	}

	private void assertUserExistsByIdOrElseThrow(Long userId) {
		if (!userRepo.existsById(userId)) throw new UserNotFoundException();
	}

	private void assertPostExistsByIdOrElseThrow(Long postId) {
		if (!postRepo.existsById(postId)) throw new PostNotFoundException();
	}
}
