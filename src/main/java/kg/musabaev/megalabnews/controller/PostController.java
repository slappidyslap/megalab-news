package kg.musabaev.megalabnews.controller;

import jakarta.validation.Valid;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.repository.projection.PostItemView;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@CrossOrigin(originPatterns = "*")
public class PostController {

	private final PostService postService;

	@PostMapping
	@PreAuthorize("hasAuthority('WRITE_POST')")
	ResponseEntity<NewOrUpdatePostResponse> savePost(@Valid @RequestBody NewOrUpdatePostRequest dto) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(postService.save(dto));
	}

	@GetMapping
	ResponseEntity<Page<PostListView>> getAllPosts(
			@PageableDefault Pageable pageable,
			@RequestParam(name = "tags", required = false) Set<String> tags) {
		return ResponseEntity.ok(postService.getAll(pageable, tags));
	}

	@GetMapping("/{postId}")
	ResponseEntity<PostItemView> getPostById(@PathVariable Long postId) {
		return ResponseEntity.ok(postService.getById(postId));
	}

	@DeleteMapping("/{postId}")
	@PreAuthorize("hasAuthority('WRITE_POST')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deletePostById(@PathVariable Long postId) {
		postService.deleteById(postId);
	}

	@PutMapping("/{postId}")
	@PreAuthorize("hasAuthority('WRITE_POST')")
	NewOrUpdatePostResponse updatePostById(
			@PathVariable Long postId,
			@Valid @RequestBody NewOrUpdatePostRequest dto) {
		return postService.update(postId, dto);
	}

	@PostMapping("/images")
	@PreAuthorize("hasAuthority('WRITE_POST')")
	ResponseEntity<Map<String, String>> uploadImageForPost(@RequestPart(name = "image") MultipartFile image) {
		return ResponseEntity.ok(postService.uploadImage(image));
	}

	@GetMapping("/images/{imageFilename}")
	ResponseEntity<Resource> getPostImageByFilename(@PathVariable String imageFilename) {
		Resource image = postService.getImageByFilename(imageFilename);
		return ResponseEntity
				.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getFilename() + "\"")
				.body(image);
	}
}
