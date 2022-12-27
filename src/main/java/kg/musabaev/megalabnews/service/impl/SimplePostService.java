package kg.musabaev.megalabnews.service.impl;

import jakarta.annotation.PostConstruct;
import kg.musabaev.megalabnews.controller.PostController;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.dto.PostPageResponse;
import kg.musabaev.megalabnews.mapper.PostDtoPostModelMapper;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.projection.PostWithoutContent;
import kg.musabaev.megalabnews.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Log4j2
@Primary
@RequiredArgsConstructor
public class SimplePostService implements PostService {

	private final PostDtoPostModelMapper postDtoPostModelMapper;
	private final PostRepo postRepo;

	public static final String postItemCacheName = "postItem";

	@Value("${app.storage.folder-name}")
	private String storageFolderName;
	@Value("${app.storage.post-image-folder-name}")
	private String postImageFolderName;

	private Path storage;

	// Этот метод выполниться после обработки аннотации @Value, что значит не будет null
	@PostConstruct
	void setUp() {
		storage = Path.of(storageFolderName, postImageFolderName);
	}

	@Override
	@Transactional
	public NewOrUpdatePostResponse save(NewOrUpdatePostRequest newOrUpdatePostRequest) {
		if (postRepo.existsByTitle(newOrUpdatePostRequest.title())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT);
		}
		Post newPost = postDtoPostModelMapper.toPostModel(newOrUpdatePostRequest);

		return postDtoPostModelMapper.toPostDto(postRepo.save(newPost));
	}

	@Override
	@Transactional(readOnly = true)
	public PostPageResponse getAll(Pageable pageable) {
		Page<PostWithoutContent> postPage = postRepo.findAllProjectedBy(PostWithoutContent.class, pageable);

		return new PostPageResponse(postPage);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(postItemCacheName)
	public Post getById(Long postId) {
		return postRepo.findById(postId).map(post -> {
			post.setTags(postRepo.findTagsByPostId(postId));
			return post;
		}).orElseThrow(() -> {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		});
	}

	@Override
	@Transactional
	@CacheEvict(postItemCacheName)
	public void deleteById(Long postId) {
		postRepo.findById(postId).ifPresentOrElse(post -> {
			postRepo.deleteById(postId);
			deleteImageInStorageIfExists(getLastPathSegmentOrNull(post.getImageUrl()));
		}, () -> {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		});
	}

	// see kg.musabaev.megalabnews.aspect.PostCachingAspect.updateCachePostItem
	@Override
	@Transactional
	public NewOrUpdatePostResponse update(Long postId, NewOrUpdatePostRequest dto) {
		return postRepo.findById(postId).map(post -> {
			String imageFilename = getLastPathSegmentOrNull(dto.imageUrl());
			String postImageFilename = getLastPathSegmentOrNull(post.getImageUrl());

			postDtoPostModelMapper.updatePostModelByPostDto(dto, post);
			// если пред. название файла не совпадает с текущим, то удаляем пред. файл
			if (postImageFilename != null && !imageFilename.equals(postImageFilename))
				deleteImageInStorageIfExists(postImageFilename);

			return postDtoPostModelMapper.toPostDto(postRepo.save(post));
		}).orElseThrow(() -> {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		});
	}

//	@CacheEvict("postImage")
	private void deleteImageInStorageIfExists(String imageFilename) {
		if (imageFilename == null) return;
		try {
			boolean isDeleted = Files.deleteIfExists(storage.resolve(imageFilename));

			if (isDeleted) log.debug("Изображение с названием {} удален", imageFilename);
		} catch (IOException e) {
			log.warn("Произошла ошибка при удалении изображения: {}", e.getMessage(), e);
		}
	}

	@Override
	public String uploadImage(MultipartFile image) {
		String originalFilename = image.getOriginalFilename();
		String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
		Path pathToSave = storage.resolve(uniqueFilename);
		String imageUrl = buildUrlForImageByMethodName(PostController.class, "getPostImageByFilename", uniqueFilename);
		try {
			image.transferTo(pathToSave);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return imageUrl;
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
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String getLastPathSegmentOrNull(String url) {
		return url != null ? url.substring(url.lastIndexOf("/") + 1) : null;
	}

	@Override
	@Transactional(readOnly = true)
//	@Cacheable("postImage")
	public Resource getImageByFilename(String imageFilename) {
		try {
			var image = new UrlResource(storage.resolve(imageFilename).toUri());

			if (!image.exists() || !image.isReadable()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			return image;
		} catch (MalformedURLException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
