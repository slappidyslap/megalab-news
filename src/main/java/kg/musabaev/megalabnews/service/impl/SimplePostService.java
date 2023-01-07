package kg.musabaev.megalabnews.service.impl;

import jakarta.annotation.PostConstruct;
import kg.musabaev.megalabnews.controller.PostController;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.exception.PostNotFoundException;
import kg.musabaev.megalabnews.mapper.PostMapper;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.CommentRepo;
import kg.musabaev.megalabnews.repository.PostRepo;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.Set;

@Service
@Log4j2
@Primary
@RequiredArgsConstructor
public class SimplePostService implements PostService {

	private final PostMapper postMapper;
	private final PostRepo postRepo;
	private final CommentRepo commentRepo;

	public static final String postListCacheName = "postList";
	public static final String postItemCacheName = "postItem";

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
			@CacheEvict(postListCacheName),
			@CacheEvict(value = postItemCacheName, key = "#result.id()")
	})
	public NewOrUpdatePostResponse save(NewOrUpdatePostRequest newOrUpdatePostRequest) {
		if (postRepo.existsByTitle(newOrUpdatePostRequest.title()))
			throw new ResponseStatusException(HttpStatus.CONFLICT);
		Post newPost = postMapper.toPostModel(newOrUpdatePostRequest);
		// FIXME author of post must be not null

		return postMapper.toPostDto(postRepo.save(newPost));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(postListCacheName)
	public Page<PostListView> getAll(Pageable pageable, Set<String> tags) {
		if (tags == null || tags.isEmpty())
			return postRepo.findAllProjectedBy(pageable);
		return postRepo.findAllByTagsIn(tags, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(postItemCacheName)
	public PostItemView getById(Long postId) {
		return postRepo.findProjectedById(postId).orElseThrow(() -> {
			throw new PostNotFoundException();
		});
	}

	@Override
	@Transactional
	@Caching(evict = {
			@CacheEvict(postItemCacheName),
			@CacheEvict(postListCacheName),
	})
	public void deleteById(Long postId) {
		Utils.assertPostExistsByIdOrElseThrow(postId);
		deleteImageInStorageIfExists(
				Utils.getLastPathSegmentOrNull(postRepo.findPostImageUrlByPostId(postId)));
		Utils.deleteCommentsRecursively(postId, commentRepo.getAllRootCommentId(postId));
		postRepo.deleteById(postId);
	}

	/**
	 * {@link kg.musabaev.megalabnews.aspect.PostCachingAspect#updateCachePostItem(JoinPoint, NewOrUpdatePostResponse)}
	 */
	@Override
	@Transactional
	@CacheEvict(postListCacheName)
	public NewOrUpdatePostResponse update(Long postId, NewOrUpdatePostRequest dto) {
		return postRepo.findById(postId).map(post -> {
			String imageFilename = Utils.getLastPathSegmentOrNull(dto.imageUrl());
			String postImageFilename = Utils.getLastPathSegmentOrNull(post.getImageUrl());

			postMapper.updatePostModelByPostDto(dto, post);
			// если пред. название файла не совпадает с текущим, то удаляем пред. файл
			if (postImageFilename != null && !imageFilename.equals(postImageFilename))
				deleteImageInStorageIfExists(postImageFilename);

			return postMapper.toPostDto(postRepo.save(post));
		}).orElseThrow(() -> {
			throw new PostNotFoundException();
		});
	}

	@Override
	public String uploadImage(MultipartFile image) {
		return Utils.uploadFileAndGetUrlFromMethodName(
				image,
				storage,
				PostController.class,
				"getPostImageByFilename");
	}

	@Override
	@Transactional(readOnly = true)
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
}
