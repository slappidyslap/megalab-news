package kg.musabaev.megalabnews.mapper;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import kg.musabaev.megalabnews.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

	@Mapping(target = "commentatorId", source = "commentator") // TODO переделать когда будет spring security
	@Mapping(target = "parentId", source = "parent.id")
	@Mapping(target = "postId", source = "post.id")
	NewOrUpdateCommentResponse toDto(Comment comment);

	Comment toModel(NewCommentRequest dto);
}
