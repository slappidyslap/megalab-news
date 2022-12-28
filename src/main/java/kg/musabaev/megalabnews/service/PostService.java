package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.dto.PostPageResponse;
import kg.musabaev.megalabnews.model.Post;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface PostService {

	NewOrUpdatePostResponse save(NewOrUpdatePostRequest newOrUpdatePostRequest);

	PostPageResponse getAll(Pageable pageable);

	Post getById(Long postId);

	void deleteById(Long postId);

	String uploadImage(MultipartFile image);

	Resource getImageByFilename(String imageFilename);

	NewOrUpdatePostResponse update(Long postId, NewOrUpdatePostRequest dto);
}
