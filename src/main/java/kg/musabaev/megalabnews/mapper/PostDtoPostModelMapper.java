package kg.musabaev.megalabnews.mapper;

import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.model.Post;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostDtoPostModelMapper {
	Post toPostModel(NewOrUpdatePostRequest newOrUpdatePostRequest);

	NewOrUpdatePostResponse toPostDto(Post post);
}
