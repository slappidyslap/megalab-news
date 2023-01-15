package kg.musabaev.megalabnews.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kg.musabaev.megalabnews.config.OpenApiConfig;
import kg.musabaev.megalabnews.dto.AddToFavouritePostsRequest;
import kg.musabaev.megalabnews.dto.UpdateUserRequest;
import kg.musabaev.megalabnews.dto.UpdateUserResponse;
import kg.musabaev.megalabnews.dto.UploadFileResponse;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.repository.projection.UserItemView;
import kg.musabaev.megalabnews.service.UserService;
import kg.musabaev.megalabnews.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static kg.musabaev.megalabnews.config.OpenApiConfig.*;
import static kg.musabaev.megalabnews.controller.PostController.REQUEST_PARAM_DESC_POST_ID;
import static kg.musabaev.megalabnews.controller.PostController.RESPONSE_DESC_IF_POST_PAGE_RECEIVED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Пользователи", description = "Методы для работы с пользователями")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@CrossOrigin(originPatterns = "*")
public class UserController {

	private final UserService userService;


	public static final String OPERATION_DESC_REQUIRE_AUTHORITY
			= """
			| Для вызова этого метода требуется ключ доступа (accessToken) с authority `WRITE_USER`. |
			|-|
			""";
	public static final String OPERATION_DESC_MUST_EQUALS_USER = """
			Для выполнение этого метода `userId` должна совпадать с тем идентификатором пользователя, \
			который авторизован. На сервере это высчитывается через `accessToken`, который вы передали \
			в заголовке запроса. В случае если `accessToken` не соответствует пользователю над которым \
			делается операция, то придет ответ с статус кодом 403.
			""";

	public static final String REQUEST_PARAM_DESC_USER_ID = "Идентификатор пользователя. Не должно быть отрицательным числом.";

	public static final String REQUEST_DESC_ADD_FAVOURITE_POST = """
			* `postId` - Идентификатор публикации. Не должно быть отрицательным числом или `null`.
			""";
	public static final String REQUEST_DESC_UPDATE_USER = """
			Если введенный `username` уже существует в базе, сгенерируется ошибка.
			* `name` - Имя автора.
			* `surname` - Фамилия автора.
			* `username` - Пользовательское имя автора.
			* `userPictureUrl` - Аватарка автора. Может быть `null` или валидный url адрес на изображение.
			""";

	public static final String RESPONSE_DESC_IF_USER_UPDATED = "Если пользователь успешно обновлен, " + THEN_RETURNED_OBJECT_WITH_FOLLOWING_FIELDS;
	public static final String RESPONSE_DESC_IF_USER_ALREADY_EXISTS = "Если пользователь с таким `username` уже существует.";
	public static final String RESPONSE_DESC_IF_USER_ITEM = """
			* `id` - Идентификатор пользователя.
			* `name` - Имя автора.
			* `surname` - Фамилия автора.
			* `username` - Пользовательское имя автора.
			* `userPictureUrl` - Аватарка автора. Может быть `null` или валидный url адрес на изображение.
			""";
	public static final String RESPONSE_DESC_IF_USER_FOUND = "Если пользователь найден, " + THEN_RETURNED_OBJECT_WITH_FOLLOWING_FIELDS;
	public static final String RESPONSE_DESC_IF_USER_OR_POST_NOT_FOUND = "Если пользователь или публикация не найдена.";
	public static final String RESPONSE_DESC_IF_USER_NOT_FOUND = "Если пользователь не найден.";
	public static final String RESPONSE_DESC_IF_USER_DELETED = "Если пользователь успешно удален.";

	@Operation(
			summary = "Возвращает информацию о конкретном пользователе.",
			responses = {
					@ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_USER_FOUND + "\n" + RESPONSE_DESC_IF_USER_ITEM),
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_USER_OR_POST_NOT_FOUND, content = @Content)})
	@GetMapping("/{userId}")
	ResponseEntity<UserItemView> getUserById(
			@Parameter(description = REQUEST_PARAM_DESC_USER_ID) @Positive @PathVariable
			Long userId
	) {
		return ResponseEntity.ok(userService.getById(userId));
	}

	@Operation(
			summary = "Добавляет публикацию в избранные конкретного пользователя.",
			description = OPERATION_DESC_REQUIRE_AUTHORITY + OPERATION_DESC_MUST_EQUALS_USER,
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = REQUEST_DESC_ADD_FAVOURITE_POST),
			responses = {
					@ApiResponse(responseCode = "200", description = "Если публикация успешно добавлена."),
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_USER_OR_POST_NOT_FOUND, content = @Content),
					@ApiResponse(responseCode = "400", description = RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID, content = @Content)})
	@PostMapping("/{userId}/favourite-posts")
	@PreAuthorize("hasAuthority('WRITE_USER')")
	void addPostToUserFavouritePosts(
			@Parameter(description = REQUEST_PARAM_DESC_USER_ID) @Positive @PathVariable
			Long userId,
			@Valid @RequestBody AddToFavouritePostsRequest dto
	) {
		userService.addToFavouritePosts(userId, dto);
	}

	@Operation(
			summary = "Удаляет публикацию из избранных у конкретного пользователя.",
			description = OPERATION_DESC_REQUIRE_AUTHORITY + OPERATION_DESC_MUST_EQUALS_USER,
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = REQUEST_DESC_ADD_FAVOURITE_POST),
			responses = {
					@ApiResponse(responseCode = "204", description = "Если публикация успешно удалена."),
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_USER_OR_POST_NOT_FOUND, content = @Content)})
	@DeleteMapping("/{userId}/favourite-posts/{postId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('WRITE_USER')")
	void deletePostFromUserFavouritePosts(
			@Parameter(description = REQUEST_PARAM_DESC_USER_ID) @Positive @PathVariable
			Long userId,
			@Parameter(description = REQUEST_PARAM_DESC_POST_ID) @Positive @PathVariable
			Long postId
	) {
		userService.deleteFromFavouritePosts(userId, postId);
	}

	@Operation(
			summary = "Возвращает постранично все избранные публикации конкретного пользователя.",
			description = OPERATION_DESC_MUST_EQUALS_USER,
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			responses = @ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_POST_PAGE_RECEIVED))
	@GetMapping("/{userId}/favourite-posts")
	ResponseEntity<Page<PostListView>> getAllFavouritePosts(
			@Parameter(description = REQUEST_PARAM_DESC_USER_ID) @Positive @PathVariable
			Long userId,
			@ParameterObject @PageableDefault Pageable pageable
	) {
		return ResponseEntity.ok(userService.getAllFavouritePostsByUserId(userId, pageable));
	}

	@Operation(
			summary = "Возвращает постранично все созданные публикации конкретного пользователя.",
			responses = @ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_POST_PAGE_RECEIVED))
	@GetMapping("/{userId}/created-posts")
	ResponseEntity<Page<PostListView>> getAllCreatedPosts(
			@Parameter(description = REQUEST_PARAM_DESC_USER_ID) @Positive @PathVariable
			Long userId,
			@ParameterObject @PageableDefault Pageable pageable
	) {
		return ResponseEntity.ok(userService.getAllCreatedPostsByUserId(userId, pageable));
	}

	@Operation(
			summary = "Редактирует конкретного пользователя.",
			description = OPERATION_DESC_REQUIRE_AUTHORITY + OPERATION_DESC_MUST_EQUALS_USER,
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = REQUEST_DESC_UPDATE_USER),
			responses = {
					@ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_USER_UPDATED + "\n" + RESPONSE_DESC_IF_USER_ITEM),
					@ApiResponse(responseCode = "409", description = RESPONSE_DESC_IF_USER_ALREADY_EXISTS, content = @Content),
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_USER_NOT_FOUND, content = @Content),
					@ApiResponse(responseCode = "400", description = RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID, content = @Content)})
	@PatchMapping("/{userId}")
	@PreAuthorize("hasAuthority('WRITE_USER')")
	ResponseEntity<UpdateUserResponse> updateUserById(
			@Parameter(description = REQUEST_PARAM_DESC_USER_ID) @Positive @PathVariable
			Long userId,
			@Valid @RequestBody UpdateUserRequest dto) {
		return ResponseEntity.ok(userService.update(userId, dto));
	}

	@Operation(
			summary = "Удаляет конкретного пользователя.",
			description =
					OPERATION_DESC_REQUIRE_AUTHORITY +
							OPERATION_DESC_MUST_EQUALS_USER +
							"При удалении, так же каскадно удаляется оставленные комментарии и созданные публикации этого пользователя.",
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			responses = {
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_USER_NOT_FOUND),
					@ApiResponse(responseCode = "204", description = RESPONSE_DESC_IF_USER_DELETED)})
	@DeleteMapping("/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('WRITE_USER')")
	void deleteUserById(@PathVariable Long userId) {
		userService.deleteById(userId);
	}

	@Operation(
			summary = "Загружает новую аватарку пользователя.",
			description = OPERATION_DESC_REQUIRE_AUTHORITY,
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			responses = @ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_FILE_UPLOADED))
	@PostMapping(path = "/user-pictures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('WRITE_USER')")
	ResponseEntity<UploadFileResponse> uploadUserPictureForUser(@RequestPart("user-picture") MultipartFile userPicture) {
		return ResponseEntity.ok(userService.uploadUserPicture(userPicture));
	}

	@Operation(
			summary = "Возвращает конкретное изображение.",
			responses = {
					@ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_FILE_RECEIVED, content = @Content),
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_FILE_NOT_FOUND, content = @Content)})
	@GetMapping("/user-pictures/{filename}")
	ResponseEntity<Resource> getUserPictureByFilename(@PathVariable String filename) {
		Resource userPicture = userService.getUserPictureByFilename(filename);
		return ResponseEntity
				.ok()
				.contentType(Utils.getMediaTypeByFilename(filename))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + userPicture.getFilename() + "\"")
				.body(userPicture);
	}
}
