package kg.musabaev.megalabnews.mapper;

import kg.musabaev.megalabnews.dto.UpdateUserRequest;
import kg.musabaev.megalabnews.dto.UpdateUserResponse;
import kg.musabaev.megalabnews.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

	UpdateUserResponse toDto(User user);

	void update(UpdateUserRequest dto, @MappingTarget User model);
}
