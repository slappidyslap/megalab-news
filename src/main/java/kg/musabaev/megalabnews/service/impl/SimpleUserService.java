package kg.musabaev.megalabnews.service.impl;

import jakarta.annotation.PostConstruct;
import kg.musabaev.megalabnews.controller.UserController;
import kg.musabaev.megalabnews.dto.AddToFavouritePostsRequest;
import kg.musabaev.megalabnews.dto.UpdateUserRequest;
import kg.musabaev.megalabnews.dto.UpdateUserResponse;
import kg.musabaev.megalabnews.exception.UserNotFoundException;
import kg.musabaev.megalabnews.mapper.UserMapper;
import kg.musabaev.megalabnews.model.User;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.UserRepo;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.service.PostService;
import kg.musabaev.megalabnews.service.UserService;
import kg.musabaev.megalabnews.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

import static kg.musabaev.megalabnews.util.Utils.assertUserExistsByIdOrElseThrow;

@Service
@RequiredArgsConstructor
@Primary
@Log4j2
public class SimpleUserService implements UserService {

	private final UserRepo userRepo;
	private final UserMapper userMapper;
	private final PostRepo postRepo;
	private final PostService postService;

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
	public User getById(Long userId) {
		return userRepo.findById(userId).orElseThrow(() -> {
			throw new UserNotFoundException();
		});
	}

	@Override
	@Transactional
	public void addToFavouritePosts(Long userId, AddToFavouritePostsRequest dto) {
		assertUserExistsByIdOrElseThrow(userId);
		Utils.assertPostExistsByIdOrElseThrow(dto.postId());

		userRepo.insertIntoFavouritePosts(userId, dto.postId());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PostListView> getAllFavouritePostsByUserId(Long userId, Pageable pageable) {
		assertUserExistsByIdOrElseThrow(userId);

		return userRepo.findFavouritePostsByUserId(userId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PostListView> getAllCreatedPostsByUserId(Long userId, Pageable pageable) {
		assertUserExistsByIdOrElseThrow(userId);

		return userRepo.findCreatedPostsByUserId(userId, pageable);
	}

	@Override
	@Transactional
	public UpdateUserResponse update(Long userId, UpdateUserRequest dto) {
		return userRepo.findById(userId).map(user -> {
			userMapper.update(dto, user);

			return userMapper.toDto(userRepo.save(user));
		}).orElseThrow(() -> {
			throw new UserNotFoundException();
		});
	}

	@Override
	@Transactional
	public String uploadUserPicture(MultipartFile userPicture) {
		return Utils.uploadFileAndGetUrlFromMethodName(
				userPicture,
				storage,
				UserController.class,
				"getUserPictureByFilename");
	}

	@Override
	@Transactional(readOnly = true)
	public Resource getUserPictureByFilename(String filename) {
		return Utils.getUploadedFileByFilenameInStorage(filename, storage);
	}

	@Override
	@Transactional
	public void deleteById(Long userId) {
		assertUserExistsByIdOrElseThrow(userId);

		for (Long postId : postRepo.findAllPostsIdByAuthorId(userId))
			postService.deleteById(postId);

		deleteImageInStorageIfExists(userRepo.findUserPictureByUserId(userId));
		userRepo.deleteById(userId);
	}

	private void deleteImageInStorageIfExists(String filename) {
		Utils.deleteFileFromStorageIfExists(
				filename,
				storage,
				() -> log.debug("Изображение пользователя с названием {} удален", filename),
				exception -> log.warn("Произошла ошибка при удалении изображения пользователя", exception));
	}
}
