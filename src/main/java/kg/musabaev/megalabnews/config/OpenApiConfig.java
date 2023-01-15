package kg.musabaev.megalabnews.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;


@Configuration
@OpenAPIDefinition(info = @Info(
		title = "Megalab News",
		description = """
				## Что это?
				Проект – REST API к новостному порталу, который необходим для сбора и выдачи \
				информации и новостей. Это сайт/платформа, которые содержат в себе инструменты \
				для взаимодействия с пользователями: публикация новостей, поиск публикаций/новостей, \
				рассылки, комментарии.

				## Параметры запросов
				* Path-параметры — параметры запроса, которые указываются прямо в строке url адреса.
				* Query-параметры — параметры запрос, последовательность пар `name=value`, разделённых \
				амперсандом, отделяясь от основного адреса знаком вопроса.

				## Доступ
				Для работы с некоторыми методами API вам необходимо передавать в заголовке \
				запросе `accessToken` — специальный многоразовый токен, который соответствует \
				отдельному пользователю в базе и становится не валидным по истечении срока. \
				Он сообщает серверу, от имени какого пользователя осуществляются запросы, \
				и какие у него есть права доступа (authority).

				Вдобавок к первому токену есть еще `refreshToken`, в отличии от первого он \
				одноразовый, но более долгоживущий. Применяется для того, чтобы получить новый \
				`accessToken` и `refreshToken`. После применения он сразу становится не валидным.

				* `accessToken` — для обращения к защищенным ресурсам.
				* `refreshToken` — для замены `accessToken`.
								
				Об необходимости о том, что уже пора менять `accessToken` указывает 401 \
				статус код ответа от защищенного ресурса. В случае если и `refreshToken` \
				тоже истек, то пользователю необходимо заново аутентифицироваться.
				""",
		version = "0.1"))
@SecurityScheme(
		name = OpenApiConfig.SECURITY_SCHEMA_NAME,
		type = SecuritySchemeType.HTTP,
		bearerFormat = "JWT",
		scheme = "Bearer")
public class OpenApiConfig {

	public static final String SECURITY_SCHEMA_NAME = "accessToken";

	public static final String RESPONSE_DESC_IF_REQUEST_BODY_NOT_VALID = "Если тело запроса не валидное.";
	public static final String THEN_RETURNED_OBJECT_WITH_FOLLOWING_FIELDS = "то возвращается объект, содержащий следующие поля:";
	public static final String RESPONSE_DESC_IF_FILE_UPLOADED = """
			Если файл успешно загружен, то возвращается объект, содержащий следующее поле:
			* `fileUrl` - Ссылка на файл.
			""";
	public static final String RESPONSE_DESC_IF_FILE_RECEIVED =
			"Если файл найден, то возвращается строковое представление изображения.";
	public static final String RESPONSE_DESC_IF_FILE_NOT_FOUND = "Если файл не найден.";

	@Bean
	public OpenApiCustomizer openAPI() {
		Schema pageSchema = ModelConverters.getInstance().readAllAsResolvedSchema(Page.class).schema;
		pageSchema.set$ref("Page");
		Map<String, Schema> pageProperties = pageSchema.getProperties();
		pageProperties.get("totalElements").description("Общее кол-во элементов.");
		pageProperties.get("totalPages").description("Общее кол-во страниц.");
		pageProperties.get("numberOfElements").description("Кол-во элементов в полученной странице.");
		pageProperties.get("size").description("Максимальное число элементов, которые могут содержаться на одной странице. Равен query-параметру `size`");
		pageProperties.get("number").description("Номер страницы. Равен query-параметру `page`");
		pageProperties.get("first").description("Является ли полученная страница первой.");
		pageProperties.get("last").description("Является ли полученная страница последней.");
		pageProperties.get("empty").description("Пустая ли полученная страница.");
		pageProperties.get("content").description("Список элементов (содержимое страницы).");
		pageProperties.get("sort").description("Метаданные о сортировке.");
		pageProperties.get("pageable").description("Метаданные о странице.");

		Schema sortSchema = ModelConverters.getInstance().readAllAsResolvedSchema(Sort.class).schema;
		Map<String, Schema> sortProperties = sortSchema.getProperties();
		sortProperties.get("empty").description("Пустой ли параметры сортировки.");
		sortProperties.get("sorted").description("Отсортирован ли элементы.");
		sortProperties.get("unsorted").description("Обратное к `sorted`.");

		Schema pageableSchema = ModelConverters.getInstance().readAllAsResolvedSchema(Pageable.class).schema;
		Map<String, Schema> pageableProperties = pageableSchema.getProperties();
		pageableProperties.get("pageNumber").description("Эквивалентен `number`.");
		pageableProperties.get("sort").description("Эквивалентен `sort`.");
		pageableProperties.get("offset").description("Смещение, необходимое для выборки определённого множества элементов. Равен по формуле `pageSize * pageNumber`.");
		pageableProperties.get("pageNumber").description("Эквивалентен `number`.");
		pageableProperties.get("pageSize").description("Эквивалентен `size`.");
		pageableProperties.get("paged").description("Разбита ли элементы на страницы.");
		pageableProperties.get("unpaged").description("Обратное к `paged`.");

		return openApi -> openApi
				.getComponents()
				.addSchemas("Page", pageSchema)
				.addSchemas("Pageable", pageableSchema)
				.addSchemas("Sort", sortSchema);
	}
}
