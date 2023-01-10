package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.dto.AuthenticateOrRefreshResponse;
import kg.musabaev.megalabnews.dto.AuthenticateRequest;
import kg.musabaev.megalabnews.dto.RegisterUserRequest;
import kg.musabaev.megalabnews.dto.RegisterUserResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

import static kg.musabaev.megalabnews.aspect.UserServiceLoggingServiceAspect.USER_BY_USERNAME_ALREADY_EXISTS;
import static kg.musabaev.megalabnews.util.Utils.*;

@Component
@Aspect
@Log4j2
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthenticationServiceLoggingAspect {

	static String USER_AUTHORIZED = "Юзер с username \"{}\" аутентифицирован";
	static String USER_UPDATED_REFRESH_TOKEN = "Юзер с username \"{}\" обновил свой access token";
	static String REFRESH_TOKEN_NOT_FOUND = "refresh token не найден";
	static String REFRESH_TOKEN_EXPIRED = "refresh token истек";
	static String USER_BY_USERNAME_NOT_FOUND = "Юзер с username \"{}\" не найден";
	static String NEW_POST_SAVED = "Новый юзер: {}";

	@Pointcut("within(kg.musabaev.megalabnews.service.AuthenticationService+)")
	void targetPackage() {
	}


	@AfterReturning("targetPackage() && execution(* authenticate(..))")
	void afterReturningMethodAuthenticate(JoinPoint jp) {
		log.debug(USER_AUTHORIZED, ((AuthenticateRequest) jp.getArgs()[0]).username());
	}

	@AfterThrowing(pointcut = "targetPackage() && execution(* authenticate(..))", throwing = "e")
	void afterThrowingMethodAuthenticate(JoinPoint jp, Exception e) {
		ifUserNotFound(e, () -> log.debug(USER_BY_USERNAME_NOT_FOUND, ((AuthenticateRequest) jp.getArgs()[0]).username()));
	}


	@AfterReturning(value = "targetPackage() && execution(* register(..))", returning = "r")
	void afterReturningMethodRegister(RegisterUserResponse r) {
		log.debug(NEW_POST_SAVED, r);
	}

	@AfterThrowing(pointcut = "targetPackage() && execution(* register(..))", throwing = "e")
	void afterThrowingMethodRegister(JoinPoint jp, Exception e) {
		ifConflict(e, () -> log.debug(USER_BY_USERNAME_ALREADY_EXISTS, ((RegisterUserRequest) jp.getArgs()[0]).username()));
	}


	@AfterReturning(value = "targetPackage() && execution(* refresh(..))", returning = "r")
	void afterReturningMethodRefresh(AuthenticateOrRefreshResponse r) {
		log.debug(USER_UPDATED_REFRESH_TOKEN, r.user().username());
	}

	@AfterThrowing(pointcut = "targetPackage() && execution(* refresh(..))", throwing = "e")
	void afterThrowingMethodRefresh(JoinPoint jp, Exception e) {
		iterateChainOfChecks(e, List.of(
				ifNotFound(e, () -> log.debug(REFRESH_TOKEN_NOT_FOUND)),
				ifResponseStatusExceptionWithStatusOrElseLog(e, HttpStatus.UNAUTHORIZED, () -> log.debug(REFRESH_TOKEN_EXPIRED)))
		);
	}
}
