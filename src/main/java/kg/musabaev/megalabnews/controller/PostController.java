package kg.musabaev.megalabnews.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kg.musabaev.megalabnews.config.OpenApiConfig;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.dto.UploadFileResponse;
import kg.musabaev.megalabnews.repository.projection.PostItemView;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.service.PostService;
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

import java.util.Set;

import static kg.musabaev.megalabnews.config.OpenApiConfig.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Публикации", description = "Методы для работы с публикациями")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@CrossOrigin(originPatterns = "*")
public class PostController {

	private final PostService postService;


	public static final String OPERATION_DESC_REQUIRE_AUTHORITY
			= """
			| Для вызова этого метода требуется ключ доступа (accessToken) с authority `WRITE_POST`. |
			|-|
			""";

	public static final String REQUEST_PARAM_DESC_POST_ID = "Идентификатор публикации. Не должно быть отрицательным числом.";
	public static final String REQUEST_PARAM_DESC_POST_TAGS = "Теги по которому фильтрируются публикации перечисленные через запятую.";
	public static final String REQUEST_PARAM_DESC_VALID_FILE = "Изображение формата jpg/jpeg или png и размер которого не превышает 1MB.";

	public static final String REQUEST_DESC_NEW_OR_UPDATE_POST = """
			Если введенный заголовок уже существует в базе, сгенерируется ошибка.
			* `title` - Заголовок публикации.
			* `description` - Описание публикации.
			* `content` - Содержимое публикации.
			* `tags` - Список уникальных тегов публикации.
			* `imageUrl` - Изображение публикации. Должно быть валидным url адрес на изображение или `null`.
			""";

	public static final String RESPONSE_DESC_IF_POST_PAGE_RECEIVED = """
			Возвращается объект Page (см. раздел Schemas) содержащий публикации.
			""";
	public static final String RESPONSE_DESC_IF_POST_ITEM = """
			* `id` - Идентификатор публикации.
			* `content` - Содержимое публикации.
			* `description` - Описание публикации.
			* `tags` - Список уникальных тегов публикации.
			* `title` - Заголовок публикации.
			* `createdDate` - Дата создание публикации в формате "yyyy-mm-dd".
			* `imageUrl` - Изображение публикации. Может быть `null` или валидный url адрес на изображение.
			* `author` - Автор публикации. Подробнее см. [Пользователи](#/Пользователи).
				* `id` - Идентификатор автора.
				* `name` - Имя автора.
				* `surname` - Фамилия автора.
				* `username` - Пользовательское имя автора.
				* `userPictureUrl` - Аватарка автора. Может быть `null` или валидный url адрес на изображение.
			""";
	public static final String RESPONSE_DESC_IF_POST_NOT_FOUND = "Если публикация не найдена.";
	public static final String RESPONSE_DESC_IF_POST_ALREADY_EXISTS = "Если публикация с таким `title` уже существует.";
	public static final String RESPONSE_DESC_IF_POST_UPDATED = "Если публикация успешно обновлена, " + THEN_RETURNED_OBJECT_WITH_FOLLOWING_FIELDS;
	public static final String RESPONSE_DESC_IF_POST_SAVED = "Если публикация успешно сохранена, " + THEN_RETURNED_OBJECT_WITH_FOLLOWING_FIELDS;
	public static final String RESPONSE_DESC_IF_POST_FOUND = "Если публикация найдена, " + THEN_RETURNED_OBJECT_WITH_FOLLOWING_FIELDS;
	public static final String RESPONSE_DESC_IF_POST_DELETED = "Если публикация успешно удалена.";

	@Operation(
			summary = "Создает новую публикацию.",
			description = OPERATION_DESC_REQUIRE_AUTHORITY,
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = REQUEST_DESC_NEW_OR_UPDATE_POST),
			responses = {
					@ApiResponse(responseCode = "201", description = RESPONSE_DESC_IF_POST_SAVED + "\n" + RESPONSE_DESC_IF_POST_ITEM),
					@ApiResponse(responseCode = "409", description = RESPONSE_DESC_IF_POST_ALREADY_EXISTS, content = @Content),
					@ApiResponse(responseCode = "400", description = RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID, content = @Content)})
	@PostMapping
	@PreAuthorize("hasAuthority('WRITE_POST')")
	ResponseEntity<NewOrUpdatePostResponse> savePost(@Valid @RequestBody NewOrUpdatePostRequest dto) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(postService.save(dto));
	}

	@Operation(
			summary = "Возвращает постранично все публикации.",
			responses = @ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_POST_PAGE_RECEIVED))
	@GetMapping
	ResponseEntity<Page<PostListView>> getAllPosts(
			@ParameterObject @PageableDefault
			Pageable pageable,
			@Parameter(
					description = REQUEST_PARAM_DESC_POST_TAGS,
					schema = @Schema(example = "Спорт,Наука", type = "array[string]"), allowEmptyValue = true)
			@RequestParam(name = "tags", required = false)
			Set<String> tags
	) {
		return ResponseEntity.ok(postService.getAll(pageable, tags));
	}

	@Operation(summary = "Возвращает информацию о конкретной публикации.", responses = {
			@ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_POST_FOUND + "\n" + RESPONSE_DESC_IF_POST_ITEM),
			@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_POST_NOT_FOUND, content = @Content)})
	@GetMapping("/{postId}")
	ResponseEntity<PostItemView> getPostById(
			@Parameter(description = REQUEST_PARAM_DESC_POST_ID) @Positive @PathVariable
			Long postId
	) {
		return ResponseEntity.ok(postService.getById(postId));
	}

	@Operation(
			summary = "Удаляет конкретную публикацию.",
			description = OPERATION_DESC_REQUIRE_AUTHORITY + "При удалении, так же каскадно удаляется комментарии этой публикации.",
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			responses = {
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_POST_NOT_FOUND),
					@ApiResponse(responseCode = "204", description = RESPONSE_DESC_IF_POST_DELETED)})
	@DeleteMapping("/{postId}")
	@PreAuthorize("hasAuthority('WRITE_POST')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deletePostById(
			@Parameter(description = REQUEST_PARAM_DESC_POST_ID) @Positive @PathVariable
			Long postId
	) {
		postService.deleteById(postId);
	}

	@Operation(
			summary = "Редактирует конкретную публикацию.",
			description = OPERATION_DESC_REQUIRE_AUTHORITY,
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = REQUEST_DESC_NEW_OR_UPDATE_POST),
			responses = {
					@ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_POST_UPDATED + "\n" + RESPONSE_DESC_IF_POST_ITEM),
					@ApiResponse(responseCode = "409", description = RESPONSE_DESC_IF_POST_ALREADY_EXISTS, content = @Content),
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_POST_NOT_FOUND, content = @Content),
					@ApiResponse(responseCode = "400", description = RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID, content = @Content)})
	@PutMapping("/{postId}")
	@PreAuthorize("hasAuthority('WRITE_POST')")
	NewOrUpdatePostResponse updatePostById(
			@Parameter(description = REQUEST_PARAM_DESC_POST_ID) @Positive @PathVariable
			Long postId,
			@Valid @RequestBody NewOrUpdatePostRequest dto
	) {
		return postService.update(postId, dto);
	}

	@Operation(
			summary = "Загружает новое изображение публикации.",
			description = OPERATION_DESC_REQUIRE_AUTHORITY,
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			responses = @ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_FILE_UPLOADED))
	@PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('WRITE_POST')")
	ResponseEntity<UploadFileResponse> uploadImageForPost(
			@Parameter(description = REQUEST_PARAM_DESC_VALID_FILE)
			@RequestPart("image")
			MultipartFile image) {
		return ResponseEntity.ok(postService.uploadImage(image));
	}

	@Operation(
			summary = "Возвращает конкретное изображение.",
			responses = {
					@ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_FILE_RECEIVED, content = @Content),
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_FILE_NOT_FOUND, content = @Content)})
	@GetMapping("/images/{imageFilename}")
	ResponseEntity<Resource> getPostImageByFilename(@PathVariable String imageFilename) {
		Resource image = postService.getImageByFilename(imageFilename);
		return ResponseEntity
				.ok()
				.contentType(Utils.getMediaTypeByFilename(imageFilename))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getFilename() + "\"")
				.body(image);
	}
}
