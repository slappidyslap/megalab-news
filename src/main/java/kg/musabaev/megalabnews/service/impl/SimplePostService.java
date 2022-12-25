package kg.musabaev.megalabnews.service.impl;

import kg.musabaev.megalabnews.dto.NewPostRequest;
import kg.musabaev.megalabnews.dto.NewPostResponse;
import kg.musabaev.megalabnews.dto.PostPageResponse;
import kg.musabaev.megalabnews.mapper.PostDtoPostModelMapper;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.projection.PostWithoutContent;
import kg.musabaev.megalabnews.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Service
@Log4j2
@Primary
@RequiredArgsConstructor
public class SimplePostService implements PostService {

	private final PostDtoPostModelMapper postDtoPostModelMapper;
	private final PostRepo postRepo;

	private final Path storage = Path.of("storage").resolve("post-image");

	@Override
	@Transactional
	public NewPostResponse save(NewPostRequest newPostRequest) {
		if (postRepo.existsByTitle(newPostRequest.title())) {
			log.debug("Публикация с title {} уже существует", newPostRequest.title());
			throw new ResponseStatusException(HttpStatus.CONFLICT);
		}
		Post newPost = postDtoPostModelMapper.toPostModel(newPostRequest);

		log.debug("Новая публикация: {}", newPost);

		return postDtoPostModelMapper.toPostDto(postRepo.save(newPost));
	}

	@Override
	@Transactional(readOnly = true)
	public PostPageResponse getAll(Pageable pageable) {
		Page<PostWithoutContent> postPage = postRepo.findAllProjectedBy(PostWithoutContent.class, pageable);

		log.debug("Общее кол-во публикаций: {}", postPage.getTotalElements());

		return new PostPageResponse(postPage);
	}

	@Override
	@Transactional(readOnly = true)
	public Post getById(Long postId) {
		Optional<Post> optionalPost = postRepo.findById(postId);

		optionalPost.ifPresent(post -> {
			post.setTags(postRepo.findTagsByPostId(postId));
			log.debug("Найдена публикация с id: {}", post);
		});

		return optionalPost.orElseThrow(() -> {
			log.debug("Публикация с id {} не найден", postId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		});
	}

	@Override
	@Transactional
	public void deleteById(Long postId) {
		if (!postRepo.existsById(postId)) {
			log.debug("Публикация с id {} не найден", postId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		log.debug("Публикации с id {} удален", postId);

		postRepo.deleteById(postId);
	}

	@Override
	public String uploadImage(MultipartFile image) {
		String originalFilename = image.getOriginalFilename();
		String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
		Path path = storage.resolve(uniqueFilename);
		try {
			image.transferTo(path);
		} catch (IOException e) {
			log.warn("Произошла ошибка при сохранении изображения: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		log.debug("Новая изображение публикации сохранен в: {}", path);

		return uniqueFilename;
	}

	@Override
	public Resource getImageByFilename(String imageFilename) {
		try {
			var image = new UrlResource(storage.resolve(imageFilename).toUri());

			log.debug("Изображение с названием {} загружен", image.getFilename());

			return image;
		} catch (IOException e) {
			log.warn("Произошла ошибка при загрузке изображения: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
