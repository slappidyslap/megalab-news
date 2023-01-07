package kg.musabaev.megalabnews.mapper;

import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PostMapper {

	Post toModel(NewOrUpdatePostRequest newOrUpdatePostRequest);

	void update(NewOrUpdatePostRequest dto, @MappingTarget Post model);

	void update(NewOrUpdatePostResponse dto, @MappingTarget Post model);

	NewOrUpdatePostResponse toDto(Post post);
}
