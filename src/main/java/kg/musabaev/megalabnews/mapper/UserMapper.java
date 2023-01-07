package kg.musabaev.megalabnews.mapper;

import kg.musabaev.megalabnews.dto.*;
import kg.musabaev.megalabnews.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

	UpdateUserResponse toUpdateUserDto(User user);

	void update(UpdateUserRequest dto, @MappingTarget User model);

	@Mapping(target = "password", ignore = true)
	User toModel(RegisterUserRequest dto);

	RegisterUserResponse toRegisterUserDto(User user);

	AuthenticateOrRefreshResponse.UserInfo toAuthResponseUserDto(User user);
}
