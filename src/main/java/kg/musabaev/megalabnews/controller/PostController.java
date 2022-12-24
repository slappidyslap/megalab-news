package kg.musabaev.megalabnews.controller;

import jakarta.validation.Valid;
import kg.musabaev.megalabnews.dto.NewPostRequest;
import kg.musabaev.megalabnews.dto.NewPostResponse;
import kg.musabaev.megalabnews.dto.PostPageResponse;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
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
}
