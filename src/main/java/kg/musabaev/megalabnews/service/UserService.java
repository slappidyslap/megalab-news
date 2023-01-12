package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.AddToFavouritePostsRequest;
import kg.musabaev.megalabnews.dto.UpdateUserRequest;
import kg.musabaev.megalabnews.dto.UpdateUserResponse;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.repository.projection.UserItemView;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService {

	UserItemView getById(Long userId);

	void addToFavouritePosts(Long userId, AddToFavouritePostsRequest dto);

	void deleteFromFavouritePosts(Long userId, Long postId);

	Page<PostListView> getAllFavouritePostsByUserId(Long userId, Pageable pageable);

	Page<PostListView> getAllCreatedPostsByUserId(Long userId, Pageable pageable);

	UpdateUserResponse update(Long userId, UpdateUserRequest dto);

	Map<String, String> uploadUserPicture(MultipartFile userPicture);

	Resource getUserPictureByFilename(String filename);

	void deleteById(Long userId);
}
