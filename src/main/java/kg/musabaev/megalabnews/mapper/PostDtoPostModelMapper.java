package kg.musabaev.megalabnews.mapper;

import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PostDtoPostModelMapper {

	Post toPostModel(NewOrUpdatePostRequest newOrUpdatePostRequest);

	void updatePostModelByPostDto(NewOrUpdatePostRequest dto, @MappingTarget Post model);

	NewOrUpdatePostResponse toPostDto(Post post);
}
