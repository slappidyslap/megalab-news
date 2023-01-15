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
import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import kg.musabaev.megalabnews.dto.UpdateCommentRequest;
import kg.musabaev.megalabnews.repository.projection.CommentListView;
import kg.musabaev.megalabnews.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static kg.musabaev.megalabnews.config.OpenApiConfig.RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID;
import static kg.musabaev.megalabnews.config.OpenApiConfig.THEN_RETURNED_OBJECT_WITH_FOLLOWING_FIELDS;
import static kg.musabaev.megalabnews.controller.PostController.REQUEST_PARAM_DESC_POST_ID;

@Tag(name = "Комментарии", description = "Методы для работы с комментариями")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@CrossOrigin(originPatterns = "*")
public class CommentController {

	private final CommentService commentService;


	public static final String OPERATION_DESC_REQUIRE_AUTHORITY
			= """
			| Для вызова этого метода требуется ключ доступа (accessToken) с authority `WRITE_COMMENT`. |
			|-|
			""";

	public static final String REQUEST_PARAM_DESC_COMMENT_ID = "Идентификатор комментария. Не должно быть отрицательным числом.";

	public static final String REQUEST_DESC_NEW_OR_UPDATE_COMMENT = """
			* `parentId` - Идентификатор родительского комментария или `null`, если создается корневой комментарий.
			* `content` - Содержимое комментария.
			""";
	public static final String REQUEST_DESC_UPDATE_COMMENT = """
			* `content` - Содержимое комментария.
			""";

	public static final String RESPONSE_DESC_IF_COMMENT_ITEM = """
			* `id` - Идентификатор комментария.
			* `postId` - Идентификатор публикации.
			* `parentId` - Идентификатор родительского комментария или `null`.
			* `content` - Содержимое комментария.
			* `createdDate` - Дата создание комментария в формате "yyyy-mm-dd".
			* `author` - Автор публикации. Подробнее см. [Пользователи](#/Пользователи).
				* `id` - Идентификатор автора.
				* `name` - Имя автора.
				* `surname` - Фамилия автора.
				* `username` - Пользовательское имя автора.
				* `userPictureUrl` - Аватарка автора. Может быть `null` или валидный url адрес на изображение.
			""";
	public static final String RESPONSE_DESC_IF_COMMENT_SAVED = "Если комментарий успешно сохранен, " + THEN_RETURNED_OBJECT_WITH_FOLLOWING_FIELDS;
	public static final String RESPONSE_DESC_IF_COMMENT_UPDATED = "Если комментарий успешно обновлен, " + THEN_RETURNED_OBJECT_WITH_FOLLOWING_FIELDS;
	public static final String RESPONSE_DESC_IF_COMMENT_NOT_FOUND = "Если комментарий не найден.";
	public static final String RESPONSE_DESC_IF_COMMENT_DELETED = "Если публикация успешно удален.";
	public static final String RESPONSE_DESC_IF_COMMENT_PAGE_RECEIVED = """
			Возвращается объект Page (см. раздел Schemas) содержащий комментарии.
			""";

	@Operation(
			summary = "Создает новый комментарий",
			description = OPERATION_DESC_REQUIRE_AUTHORITY,
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = REQUEST_DESC_NEW_OR_UPDATE_COMMENT),
			responses = {
					@ApiResponse(responseCode = "201", description = RESPONSE_DESC_IF_COMMENT_SAVED + "\n" + RESPONSE_DESC_IF_COMMENT_ITEM),
					@ApiResponse(responseCode = "400", description = RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID, content = @Content)})
	@PostMapping("/{postId}/comments")
	@PreAuthorize("hasAuthority('WRITE_COMMENT')")
	ResponseEntity<NewOrUpdateCommentResponse> saveComment(
			@Parameter(description = REQUEST_PARAM_DESC_POST_ID) @Positive @PathVariable
			Long postId,
			@Valid @RequestBody NewCommentRequest dto
	) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body((commentService.save(postId, dto)));
	}

	@Operation(
			summary = "Возвращает постранично все корневые комментарии конкретной публикации.",
			responses = @ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_COMMENT_PAGE_RECEIVED))
	@GetMapping("/{postId}/comments")
	ResponseEntity<Page<CommentListView>> getRootCommentsOfPostById(
			@Parameter(description = REQUEST_PARAM_DESC_POST_ID) @Positive @PathVariable
			Long postId,
			@ParameterObject @PageableDefault Pageable pageable
	) {
		return ResponseEntity.ok(commentService.getRootsByPostId(postId, pageable));
	}

	@Operation(
			summary = "Возвращает постранично все дочерние комментарии конкретной публикации и родительского комментария.",
			responses = @ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_COMMENT_PAGE_RECEIVED))
	@GetMapping("/{postId}/comments/{parentCommentId}")
	ResponseEntity<Page<CommentListView>> getCommentChildrenOfParentId(
			@Parameter(description = REQUEST_PARAM_DESC_POST_ID) @Positive @PathVariable
			Long postId,
			@Parameter(description = REQUEST_PARAM_DESC_COMMENT_ID) @Positive @PathVariable
			Long parentCommentId,
			@ParameterObject @PageableDefault Pageable pageable
	) {
		return ResponseEntity.ok(commentService.getChildrenByParentId(postId, parentCommentId, pageable));
	}

	@Operation(
			summary = "Редактирует конкретный комментарий.",
			description = OPERATION_DESC_REQUIRE_AUTHORITY,
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = REQUEST_DESC_UPDATE_COMMENT),
			responses = {
					@ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_COMMENT_UPDATED + "\n" + RESPONSE_DESC_IF_COMMENT_ITEM),
					@ApiResponse(responseCode = "400", description = RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID, content = @Content),
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_COMMENT_NOT_FOUND, content = @Content),})
	@PatchMapping("/{postId}/comments/{commentId}")
	@PreAuthorize("hasAuthority('WRITE_COMMENT')")
	ResponseEntity<NewOrUpdateCommentResponse> updateCommentById(
			@Parameter(description = REQUEST_PARAM_DESC_POST_ID) @Positive @PathVariable
			Long postId,
			@Parameter(description = REQUEST_PARAM_DESC_COMMENT_ID) @Positive @PathVariable
			Long commentId,
			@Valid @RequestBody UpdateCommentRequest dto
	) {
		return ResponseEntity.ok(commentService.update(postId, commentId, dto));
	}

	@Operation(
			summary = "Удаляет конкретный комментарий.",
			description = OPERATION_DESC_REQUIRE_AUTHORITY + "При удалении, так же каскадно удалется дочерние комментарии",
			security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEMA_NAME),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = REQUEST_DESC_NEW_OR_UPDATE_COMMENT),
			responses = {
					@ApiResponse(responseCode = "204", description = RESPONSE_DESC_IF_COMMENT_DELETED),
					@ApiResponse(responseCode = "404", description = RESPONSE_DESC_IF_COMMENT_NOT_FOUND, content = @Content),})
	@DeleteMapping("/{postId}/comments/{commentId}")
	@PreAuthorize("hasAuthority('WRITE_COMMENT')")
	void deleteCommentById(
			@Parameter(description = REQUEST_PARAM_DESC_POST_ID) @Positive @PathVariable
			Long postId,
			@Parameter(description = REQUEST_PARAM_DESC_COMMENT_ID) @Positive @PathVariable
			Long commentId
	) {
		commentService.deleteById(postId, commentId);
	}
}
