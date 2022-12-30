package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.repository.projection.PostListView;
import kg.musabaev.megalabnews.util.Utils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

import static kg.musabaev.megalabnews.util.Utils.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

// Это бы хорошо протестировать. Потому что это культурно!
@Component
@Aspect
@Log4j2
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SimplePostServiceLoggingAspect {

	static String NEW_POST_SAVED = "Новая публикация: {}";
	static String FILE_NOT_VALID_IMAGE_FORMAT = "Изображение не валидного формата";
	static String POST_BY_ID_NOT_FOUND = "Публикация с id {} не найден";
	static String POST_BY_TITLE_ALREADY_EXISTS = "Публикация с title \"{}\" уже существует";
	static String TOTAL_NUMBER_POSTS = "Общее кол-во публикаций: {}";
	static String POST_BY_ID_FOUND = "Найдена публикация с id: {}";
	static String POST_BY_ID_DELETED = "Публикации с id {} удален";
	static String POST_BY_ID_UPDATED = "Публикация с id {} обновлен";
	static String NEW_IMAGE_SAVED_WITH_URL = "Новая изображение публикации сохранен по след. url: {}";
	static String ERROR_OCCURRED_WHILE_SAVING_IMAGE = "Произошла ошибка при сохранении изображения:";
	static String IMAGE_WITH_FILENAME_RECEIVED = "Изображение с названием {} получен";
	static String IMAGE_BY_FILENAME_NOT_FOUND = "Изображение с названием {} не найден";
	static String ERROR_OCCURRED_WHILE_RECEIVING_IMAGE = "Произошла ошибка при получении изображения:";

	@Pointcut("within(kg.musabaev.megalabnews.service.impl.SimplePostService)")
	void targetPackage() {}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* save(..)))",
			returning = "r")
	void afterReturningMethodSave(NewOrUpdatePostResponse r) {
		log.debug(NEW_POST_SAVED, r);
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* save(..)))",
			throwing = "e")
	void afterReturningMethodSave(JoinPoint jp, Exception e) {
		Utils.ifResponseStatusExceptionWithStatusOrElseLog(e, HttpStatus.CONFLICT, () -> {
			log.debug(POST_BY_TITLE_ALREADY_EXISTS, ((NewOrUpdatePostRequest) jp.getArgs()[0]).title());
		});
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* getAll(..)))",
			returning = "r")
	void afterReturningMethodGetAll(Page<PostListView> r) {
		log.debug(TOTAL_NUMBER_POSTS, r.getTotalElements());
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* getById(..)))",
			returning = "r")
	void afterReturningMethodGetById(Post r) {
		log.debug(POST_BY_ID_FOUND, r.getId());
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* getById(..))",
			throwing = "e"
	)
	void afterThrowingMethodGetById(JoinPoint jp, Exception e) {
		Utils.ifPostNotFound(e, () -> log.debug(POST_BY_ID_NOT_FOUND, jp.getArgs()[0]));
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* deleteById(..)))")
	void afterReturningMethodDeleteById(JoinPoint jp) {
		log.debug(POST_BY_ID_DELETED, jp.getArgs()[0]);
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* deleteById(..))",
			throwing = "e"
	)
	void afterThrowingMethodDeleteById(JoinPoint jp, Exception e) {
		Utils.ifPostNotFound(e, () -> log.debug(POST_BY_ID_NOT_FOUND, jp.getArgs()[0]));
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* update(..)))")
	void afterReturningMethodUpdate(JoinPoint jp) {
		log.debug(POST_BY_ID_UPDATED, jp.getArgs()[0]);
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* update(..))",
			throwing = "e"
	)
	void afterThrowingMethodUpdate(JoinPoint jp, Exception e) {
		Utils.ifPostNotFound(e, () -> log.debug(POST_BY_ID_NOT_FOUND, jp.getArgs()[0]));
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* uploadImage(..)))",
			returning = "r")
	void afterReturningMethodUploadImage(String r) {
		log.debug(NEW_IMAGE_SAVED_WITH_URL, r);
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* uploadImage(..))",
			throwing = "e"
	)
	void afterThrowingMethodUploadImage(Exception e) {
		iterateChainOfChecks(e, List.of(
				Utils.ifResponseStatusExceptionWithStatusOrElseLog(e, BAD_REQUEST,() -> {
					log.debug(FILE_NOT_VALID_IMAGE_FORMAT);
				}),
				ifInternalServerError(e, () -> log.warn(ERROR_OCCURRED_WHILE_SAVING_IMAGE, e))
		));
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* getImageByFilename(..)))",
			returning = "r")
	void afterReturningMethodGetImageByFilename(Resource r) {
		log.debug(IMAGE_WITH_FILENAME_RECEIVED, r.getFilename());
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* getImageByFilename(..))",
			throwing = "e"
	)
	void afterThrowingMethodGetImageByFilename(JoinPoint jp, Exception e) {
		iterateChainOfChecks(e,List.of(
				ifNotFound(e, () -> log.debug(IMAGE_BY_FILENAME_NOT_FOUND, jp.getArgs()[0])),
				ifInternalServerError(e, () -> log.warn(ERROR_OCCURRED_WHILE_RECEIVING_IMAGE, e))
		));
	}
}
