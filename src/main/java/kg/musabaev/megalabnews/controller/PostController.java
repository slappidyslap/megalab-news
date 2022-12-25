package kg.musabaev.megalabnews.controller;

import jakarta.validation.Valid;
import kg.musabaev.megalabnews.dto.NewPostRequest;
import kg.musabaev.megalabnews.dto.NewPostResponse;
import kg.musabaev.megalabnews.dto.PostPageResponse;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@CrossOrigin(originPatterns = "*")
public class PostController {

	private final PostService postService;

	@PostMapping
	ResponseEntity<NewPostResponse> savePost(@Valid @RequestBody NewPostRequest dto) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(postService.save(dto));
	}

	@GetMapping
	ResponseEntity<PostPageResponse> getAllPosts(@PageableDefault(5) Pageable pageable) {
		return ResponseEntity.ok(postService.getAll(pageable));
	}

	@GetMapping("/{postId}")
	ResponseEntity<Post> getPostById(@PathVariable Long postId) {
		return ResponseEntity.ok(postService.getById(postId));
	}

	@DeleteMapping("/{postId}")
	@ResponseStatus(HttpStatus.OK)
	void deletePostById(@PathVariable Long postId) {
		postService.deleteById(postId);
	}

	@PostMapping("/images")
	ResponseEntity<String> uploadImageForPost(@RequestPart(name = "image") MultipartFile image) {
		return ResponseEntity.ok(postService.uploadImage(image));
	}

	@GetMapping("/images/{imageFilename}")
	ResponseEntity<Resource> getPostImageByFilename(@PathVariable String imageFilename) {
		Resource image = postService.getImageByFilename(imageFilename);
		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getFilename() + "\"")
				.body(image);
	}
}
