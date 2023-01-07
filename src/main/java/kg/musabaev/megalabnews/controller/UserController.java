package kg.musabaev.megalabnews.controller;

import kg.musabaev.megalabnews.dto.AddToFavouritePostsRequest;
import kg.musabaev.megalabnews.dto.UpdateUserRequest;
import kg.musabaev.megalabnews.dto.UpdateUserResponse;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.repository.projection.UserItemView;
import kg.musabaev.megalabnews.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@CrossOrigin(originPatterns = "*")
public class UserController {

	private final UserService userService;

	@GetMapping("/{userId}")
	ResponseEntity<User> getUserById(@PathVariable Long userId) {
		return ResponseEntity.ok(userService.getById(userId));
	}

	@PostMapping("/{userId}/favourite-posts") // FIXME pass userId via jwt
	void addPostToUserFavouritePosts(@PathVariable Long userId, @RequestBody AddToFavouritePostsRequest dto) {
		userService.addToFavouritePosts(userId, dto);
	}

	@DeleteMapping("/{userId}/favourite-posts/{postId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deletePostFromUserFavouritePosts(@PathVariable Long userId, @PathVariable Long postId) {
		userService.deleteFromFavouritePosts(userId, postId);
	}

	@GetMapping("/{userId}/favourite-posts")
	ResponseEntity<Page<PostListView>> getAllFavouritePosts(
			@PathVariable Long userId,
			@PageableDefault Pageable pageable) {
		return ResponseEntity.ok(userService.getAllFavouritePostsByUserId(userId, pageable));
	}

	@GetMapping("/{userId}/created-posts")
	ResponseEntity<Page<PostListView>> getAllCreatedPosts(
			@PathVariable Long userId,
			@PageableDefault Pageable pageable) {
		return ResponseEntity.ok(userService.getAllCreatedPostsByUserId(userId, pageable));
	}

	@PutMapping("/{userId}")
	ResponseEntity<UpdateUserResponse> updateUserById(
			@PathVariable Long userId,
			@RequestBody UpdateUserRequest dto) {
		return ResponseEntity.ok(userService.update(userId, dto));
	}

	@DeleteMapping("/{userId}")
	void deleteUserById(@PathVariable Long userId) {
		userService.deleteById(userId);
	}

	@PostMapping("/user-pictures")
	ResponseEntity<String> uploadUserPictureForUser(@RequestPart("user-picture") MultipartFile userPicture) {
		return ResponseEntity.ok(userService.uploadUserPicture(userPicture));
	}

	@GetMapping("/user-pictures/{filename}")
	ResponseEntity<Resource> getUserPictureByFilename(@PathVariable String filename) {
		return ResponseEntity.ok(userService.getUserPictureByFilename(filename));
	}
}
