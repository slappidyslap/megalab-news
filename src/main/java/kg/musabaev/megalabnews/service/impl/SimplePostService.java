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
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

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
	public static final Set<String> validImageFormats = Set.of(
			MediaType.IMAGE_JPEG_VALUE,
			MediaType.IMAGE_PNG_VALUE
	);

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
	public Post getById(Long postId) {
		return postRepo.findById(postId).map(post -> {
			post.setTags(postRepo.findTagsByPostId(postId));
			return post;
		}).orElseThrow(() -> {
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
				getLastPathSegmentOrNull(postRepo.getPostImageUrlByPostId(postId)));
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
			String imageFilename = getLastPathSegmentOrNull(dto.imageUrl());
			String postImageFilename = getLastPathSegmentOrNull(post.getImageUrl());

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
		if (!isValidImageFormat(image))
			throw new ResponseStatusException(BAD_REQUEST);
		String uniqueFilename = getUniqueFilename(image);
		Path pathToSave = storage.resolve(uniqueFilename);
		String imageUrl = buildUrlForImageByMethodName(PostController.class, "getPostImageByFilename", uniqueFilename);
		try {
			image.transferTo(pathToSave);
		} catch (IOException e) {
			throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "", e);
		}
		return imageUrl;
	}

	@Override
	@Transactional(readOnly = true)
	public Resource getImageByFilename(String imageFilename) {
		try {
			var image = new UrlResource(storage.resolve(imageFilename).toUri());

			if (!image.exists() || !image.isReadable()) {
				throw new ResponseStatusException(NOT_FOUND);
			}
			return image;
		} catch (MalformedURLException e) {
			throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "", e);
		}
	}

	private String getUniqueFilename(MultipartFile image) {
		return UUID.randomUUID() + "_" + image.getOriginalFilename();
	}

	private boolean isValidImageFormat(MultipartFile image) {
		String imageFormat;
		try {
			imageFormat = Files.probeContentType(Path.of(image.getOriginalFilename()));
		} catch (IOException e) {
			log.warn("Произошла ошибка при получение формата изображения", e);
			throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "", e);
		}
		return validImageFormats.contains(imageFormat);
	}

	private void deleteImageInStorageIfExists(String imageFilename) {
		if (imageFilename == null) return;
		try {
			boolean isDeleted = Files.deleteIfExists(storage.resolve(imageFilename));

			if (isDeleted) log.debug("Изображение с названием {} удален", imageFilename);
		} catch (IOException e) {
			log.warn("Произошла ошибка при удалении изображения", e);
		}
	}


	private String buildUrlForImageByMethodName(Class<?> controller, String methodName, String filename) {
		try {
			return MvcUriComponentsBuilder.fromMethodName(
					controller,
					methodName,
					filename
			).toUriString();
		} catch (IllegalArgumentException e) {
			log.warn("Не удалось найти метод у %s с именем %s".formatted(PostController.class, methodName), e);
			throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "", e);
		}
	}

	private String getLastPathSegmentOrNull(String url) {
		return url != null ? url.substring(url.lastIndexOf("/") + 1) : null;
	}
}
