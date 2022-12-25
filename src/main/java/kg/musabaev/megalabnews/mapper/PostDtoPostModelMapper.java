package kg.musabaev.megalabnews.mapper;

import kg.musabaev.megalabnews.dto.NewPostRequest;
import kg.musabaev.megalabnews.dto.NewPostResponse;
import kg.musabaev.megalabnews.model.Post;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostDtoPostModelMapper {
	Post toPostModel(NewPostRequest newPostRequest);

	NewPostResponse toPostDto(Post post);
}
