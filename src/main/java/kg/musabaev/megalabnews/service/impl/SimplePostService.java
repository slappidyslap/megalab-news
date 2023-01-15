package kg.musabaev.megalabnews.service.impl;

import jakarta.annotation.PostConstruct;
import kg.musabaev.megalabnews.controller.PostController;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.dto.UploadFileResponse;
import kg.musabaev.megalabnews.exception.PostNotFoundException;
import kg.musabaev.megalabnews.exception.ResponseStatusConflictException;
import kg.musabaev.megalabnews.mapper.PostMapper;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.CommentRepo;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.UserRepo;
import kg.musabaev.megalabnews.repository.projection.PostItemView;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.service.PostService;
import kg.musabaev.megalabnews.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static kg.musabaev.megalabnews.service.impl.SimpleUserService.USER_CREATED_POSTS_CACHE_NAME;
import static kg.musabaev.megalabnews.service.impl.SimpleUserService.USER_FAVOURITE_POSTS_CACHE_NAME;
import static kg.musabaev.megalabnews.util.Utils.getLastPathSegmentOrNull;

@Service
@Log4j2
@Primary
@RequiredArgsConstructor
public class SimplePostService implements PostService {

	public static final String POST_LIST_CACHE_NAME = "postList";
	public static final String POST_ITEM_CACHE_NAME = "postItem";
	public static final String POST_IMAGE_CACHE_NAME = "postImage";

	private final PostMapper postMapper;
	private final PostRepo postRepo;
	private final UserRepo userRepo;
	private final CommentRepo commentRepo;

	@Value("${app.storage.folder-name}")
	private String storageFolderName;
	@Value("${app.storage.post-image-folder-name}")
	private String postImageFolderName;
	private Path storage;

	@PostConstruct
	void setUp() {
		storage = Path.of(storageFolderName, postImageFolderName);
	}

	@Override
	@Transactional
	@Caching(evict = {
			@CacheEvict(POST_LIST_CACHE_NAME), //FIXME
			@CacheEvict(value = POST_ITEM_CACHE_NAME, key = "#result.id()")})
	public NewOrUpdatePostResponse save(NewOrUpdatePostRequest newOrUpdatePostRequest) {
		if (postRepo.existsByTitle(newOrUpdatePostRequest.title()))
			throw new ResponseStatusConflictException();
		Post newPost = postMapper.toModel(newOrUpdatePostRequest);

		return postMapper.toDto(postRepo.save(newPost));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(POST_LIST_CACHE_NAME)
	public Page<PostListView> getAll(Pageable pageable, Set<String> tags) {
		if (tags == null || tags.isEmpty())
			return postRepo.findAllProjectedBy(pageable);
		return postRepo.findAllByTagsIn(tags, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(POST_ITEM_CACHE_NAME)
	public PostItemView getById(Long postId) {
		return postRepo.findProjectedById(postId).orElseThrow(() -> {
			throw new PostNotFoundException();
		});
	}

	@Override
	@Transactional
	@Caching(evict = {
			@CacheEvict(POST_ITEM_CACHE_NAME),
			@CacheEvict(cacheNames = POST_LIST_CACHE_NAME, allEntries = true),
			@CacheEvict(cacheNames = POST_IMAGE_CACHE_NAME, allEntries = true),
			@CacheEvict(cacheNames = USER_CREATED_POSTS_CACHE_NAME, allEntries = true), // FIXME
			@CacheEvict(cacheNames = USER_FAVOURITE_POSTS_CACHE_NAME, allEntries = true)})
	public void deleteById(Long postId) {
		assertPostExistsByIdOrElseThrow(postId);
		deleteImageInStorageIfExists(
				getLastPathSegmentOrNull(postRepo.findPostImageUrlByPostId(postId)));
		deleteCommentsRecursively(postId, commentRepo.getAllRootCommentId(postId));
		userRepo.deletePostsFromUserFavouritePosts(postId);

		postRepo.deleteById(postId);
	}

	/**
	 * {@link kg.musabaev.megalabnews.aspect.PostCachingAspect#updateCachePostItem(JoinPoint, NewOrUpdatePostResponse)}
	 */
	@Override
	@Transactional
	@Caching(evict = {
			@CacheEvict(cacheNames = POST_LIST_CACHE_NAME, allEntries = true),
			@CacheEvict(cacheNames = POST_IMAGE_CACHE_NAME, allEntries = true)})
	public NewOrUpdatePostResponse update(Long postId, NewOrUpdatePostRequest dto) {
		return postRepo.findById(postId).map(post -> {
			if (postRepo.existsByTitle(dto.title())) throw new ResponseStatusConflictException();

			// если пред. название файла не совпадает с текущим, то удаляем пред. файл
			String imageFilename = getLastPathSegmentOrNull(dto.imageUrl());
			String postImageFilename = getLastPathSegmentOrNull(post.getImageUrl());
			if (postImageFilename != null && !imageFilename.equals(postImageFilename))
				deleteImageInStorageIfExists(postImageFilename);

			postMapper.update(dto, post);

			return postMapper.toDto(postRepo.save(post));
		}).orElseThrow(() -> {
			throw new PostNotFoundException();
		});
	}

	@Override
	public UploadFileResponse uploadImage(MultipartFile image) {
		return new UploadFileResponse(Utils.uploadFileAndGetUrlFromMethodName(
				image,
				storage,
				PostController.class,
				"getPostImageByFilename"));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(POST_IMAGE_CACHE_NAME)
	public Resource getImageByFilename(String imageFilename) {
		return Utils.getUploadedFileByFilenameInStorage(imageFilename, storage);
	}

	private void deleteImageInStorageIfExists(String imageFilename) {
		Utils.deleteFileFromStorageIfExists(
				imageFilename,
				storage,
				() -> log.debug("Изображение с названием {} удален", imageFilename),
				exception -> log.warn("Произошла ошибка при удалении изображения", exception));
	}

	private void assertPostExistsByIdOrElseThrow(Long postId) {
		if (!postRepo.existsById(postId)) throw new PostNotFoundException();
	}

	private void deleteCommentsRecursively(Long postId, List<Long> commentsId) {
		if (commentsId.isEmpty()) return;
		for (Long commentId : commentsId) {
			deleteCommentsRecursively(
					postId, commentRepo.getAllChildCommentIdByParentId(postId, commentId));
			commentRepo.deleteById(commentId);
		}
	}
}
