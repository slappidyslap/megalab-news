package kg.musabaev.megalabnews.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.musabaev.megalabnews.dto.*;
import kg.musabaev.megalabnews.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static kg.musabaev.megalabnews.config.OpenApiConfig.RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID;

@Tag(name = "Доступ")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class AuthenticationController {

	private final AuthenticationService authService;

	public static final String REQUEST_DESC_AUTHENTICATE_USER = """
			* `username` - Пользовательское имя пользователя.
			* `password` - Пароль от данного пользователя.
			""";
	public static final String REQUEST_DESC_REGISTER_USER = """
			* `name` - Имя пользователя.
			* `surname` - Фамилия пользователя.
			* `username` - Уникальный пользовательское имя пользователя.
			* `password` - Пароль от данного пользователя.
			""";

	public static final String RESPONSE_DESC_IF_USER_AUTHENTICATED = """
			Если пользователь успешно авторизован, то возвращается объект, содержащий следующие поля:
						
			* `accessToken` - Токен доступа для обращения к защищенным ресурсам.
			* `refreshToken` - Токен для замены `accessToken`.
			* `user` - Пользователь, которому соответствует `accessToken`. 
				* `id` - Идентификатор пользователя.
				* `name` - Имя пользователя.
				* `surname` - Фамилия пользователя.
				* `username` - Пользовательское имя пользователя.
				* `authorities` - Права пользователя.
				* `userPictureUrl` - Аватарка пользователя. Может быть `null` или валидный URL адрес на изображение.
						
			""";
	public static final String RESPONSE_DESC_IF_USER_REGISTERED = """
			Если пользователь успешно зарегистрирован, то возвращается объект, содержащий следующие поля:
						
			* `id` - Идентификатор пользователя.
			* `name` - Имя пользователя.
			* `surname` - Фамилия пользователя.
			* `username` - Пользовательское имя пользователя.
			* `authorities` - Права пользователя.
			""";
	public static final String RESPONSE_DESC_IF_USER_NOT_AUTHENTICATED = "Если авторизация пользователя провалилась.";
	public static final String RESPONSE_DESC_IF_USER_ALREADY_EXISTS = "Если пользователь с таким `username` уже существует.";

	@Operation(
			summary = "Авторизовывает пользователя.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = REQUEST_DESC_AUTHENTICATE_USER),
			responses = {
					@ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_USER_AUTHENTICATED),
					@ApiResponse(responseCode = "400", description = RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID, content = @Content),
					@ApiResponse(responseCode = "401", description = RESPONSE_DESC_IF_USER_NOT_AUTHENTICATED, content = @Content)})
	@PostMapping("/authenticate")
	ResponseEntity<AuthenticateOrRefreshResponse> authenticate(@Valid @RequestBody AuthenticateRequest dto) {
		return ResponseEntity.ok(authService.authenticate(dto));
	}

	@Operation(
			summary = "Регистрирует нового пользователя.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = REQUEST_DESC_REGISTER_USER),
			responses = {
					@ApiResponse(responseCode = "201", description = RESPONSE_DESC_IF_USER_REGISTERED),
					@ApiResponse(responseCode = "400", description = RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID, content = @Content),
					@ApiResponse(responseCode = "409", description = RESPONSE_DESC_IF_USER_ALREADY_EXISTS, content = @Content)})
	@PostMapping("/register")
	ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest dto) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(authService.register(dto));
	}

	@Operation(
			summary = "Выдает новый `accessToken`.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "* `refreshToken` - Токен для замены `accessToken`."),
			responses = {
					@ApiResponse(responseCode = "200", description = RESPONSE_DESC_IF_USER_REGISTERED),
					@ApiResponse(responseCode = "400", description = RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID, content = @Content),
					@ApiResponse(responseCode = "404", description = "Если `resfeshToken` не найден.", content = @Content)})
	@PostMapping("/refresh")
	public ResponseEntity<AuthenticateOrRefreshResponse> refresh(@Valid @RequestBody UpdateTokenRequest dto) {
		return ResponseEntity.ok(authService.refresh(dto));
	}

}
