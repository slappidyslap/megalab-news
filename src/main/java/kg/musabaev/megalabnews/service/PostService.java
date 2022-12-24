package kg.musabaev.megalabnews.service;

import kg.musabaev.megalabnews.dto.NewPostRequest;
import kg.musabaev.megalabnews.dto.NewPostResponse;
import kg.musabaev.megalabnews.dto.PostPageResponse;
import kg.musabaev.megalabnews.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface PostService {

    NewPostResponse save(NewPostRequest newPostRequest);

    PostPageResponse getAll(Pageable pageable);

    Post getById(Long postId);

    void deleteById(Long postId);
}
