package kg.musabaev.megalabnews.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;


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
		name = "accessToken",
		type = SecuritySchemeType.HTTP,
		bearerFormat = "JWT",
		scheme = "Bearer")
public class OpenApiConfig {
}
