package kg.musabaev.megalabnews.mapper;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import kg.musabaev.megalabnews.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

	@Mapping(target = "parentId", source = "parent.id")
	@Mapping(target = "postId", source = "post.id")
	@Mapping(target = "author.id", source = "author.id")
	@Mapping(target = "author.username", source = "author.username")
	@Mapping(target = "author.name", source = "author.name")
	@Mapping(target = "author.surname", source = "author.surname")
	@Mapping(target = "author.userPictureUrl", source = "author.userPictureUrl")
	NewOrUpdateCommentResponse toDto(Comment comment);

	Comment toModel(NewCommentRequest dto);
}
