package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public interface PostService {

	NewOrUpdatePostResponse save(NewOrUpdatePostRequest newOrUpdatePostRequest);

	Page<PostListView> getAll(Pageable pageable, Set<String> tags);

	Post getById(Long postId);

	void deleteById(Long postId);

	String uploadImage(MultipartFile image);

	Resource getImageByFilename(String imageFilename);

	NewOrUpdatePostResponse update(Long postId, NewOrUpdatePostRequest dto);
}
