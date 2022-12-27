package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.dto.PostPageResponse;
import kg.musabaev.megalabnews.model.Post;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

// Это бы хорошо протестировать. Потому что это культурно!
@Component
@Aspect
@Log4j2
public class SimplePostServiceLoggingAspect {

	@Pointcut("within(kg.musabaev.megalabnews.service.impl.SimplePostService)")
	void targetPackage() {}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* save(..)))",
			returning = "r")
	void afterReturningMethodSave(NewOrUpdatePostResponse r) {
		log.debug("Новая публикация: {}", r);
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* save(..)))",
			throwing = "e")
	void afterReturningMethodaSave(JoinPoint jp, Exception e) {
		ifResponseStatusExceptionWithStatusElseLog(e, HttpStatus.CONFLICT, () -> {
			log.debug("Публикация с title \"{}\" уже существует", ((NewOrUpdatePostRequest) jp.getArgs()[0]).title());
		});
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* getAll(..)))",
			returning = "r")
	void afterReturningMethodGetAll(PostPageResponse r) {
		log.debug("Общее кол-во публикаций: {}", r.page().getTotalElements());
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* getById(..)))",
			returning = "r")
	void afterReturningMethodGetById(Post r) {
		log.debug("Найдена публикация с id: {}", r.getId());
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* getById(..))",
			throwing = "e"
	)
	void afterThrowingMethodGetById(JoinPoint jp, Exception e) {
		ifNotFound(jp, e, "Публикация с id {} не найден");
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* deleteById(..)))")
	void afterReturningMethodDeleteById(JoinPoint jp) {
		log.debug("Публикации с id {} удален", jp.getArgs()[0]);
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* deleteById(..))",
			throwing = "e"
	)
	void afterThrowingMethodDeleteById(JoinPoint jp, Exception e) {
		ifNotFound(jp, e, "Публикация с id {} не найден");
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* update(..)))")
	void afterReturningMethodUpdate(JoinPoint jp) {
		log.debug("Публикация с id {} обновлен", jp.getArgs()[0]);
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* update(..))",
			throwing = "e"
	)
	void afterThrowingMethodUpdate(JoinPoint jp, Exception e) {
		ifNotFound(jp, e, "Публикация с id {} не найден");
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* uploadImage(..)))",
			returning = "r")
	void afterReturningMethodUploadImage(String r) {
		log.debug("Новая изображение публикации сохранен по след. url: {}", r);
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* uploadImage(..))",
			throwing = "e"
	)
	void afterThrowingMethodUploadImage(Exception e) {
		ifInternalServerError(e, "Произошла ошибка при сохранении изображения");
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* getImageByFilename(..)))",
			returning = "r")
	void afterReturningMethodGetImageByFilename(Resource r) {
		log.debug("Изображение с названием {} получен", r.getFilename());
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* getImageByFilename(..))",
			throwing = "e"
	)
	void afterThrowingMethodGetImageByFilename(JoinPoint jp, Exception e) {
		ifInternalServerError(e, "Произошла ошибка при получении изображения");
		ifNotFound(jp, e, "Изображение с названием {} не найден");
	}

	// частный случай декоратора
	private void ifInternalServerError(Exception e, String str) {
		ifResponseStatusExceptionWithStatusElseLog(e, NOT_FOUND, () -> log.debug(str));
	}

	private void ifNotFound(JoinPoint jp, Exception e, String str) {
		ifResponseStatusExceptionWithStatusElseLog(e, NOT_FOUND, () -> log.debug(str, jp.getArgs()[0]));
	}

	private void ifResponseStatusExceptionWithStatusElseLog(Exception e, HttpStatus status, Runnable runnable) {
		if (e.getClass() == ResponseStatusException.class &&
				((ResponseStatusException) e).getStatusCode() == HttpStatusCode.valueOf(status.value()))
			runnable.run();
		else log.warn("Произошло другое исключение:", e);
	}
}
