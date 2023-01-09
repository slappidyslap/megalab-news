package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.dto.AddToFavouritePostsRequest;
import kg.musabaev.megalabnews.dto.UpdateUserRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

import static kg.musabaev.megalabnews.aspect.PostServiceLoggingAspect.POST_BY_ID_NOT_FOUND;
import static kg.musabaev.megalabnews.util.Utils.*;

@Component
@Aspect
@Log4j2
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserServiceLoggingServiceAspect {

	static String POST_ALREADY_EXISTS_IN_FAVOURITES = "Публикация существует в избранном у юзера {}";
	static String USER_BY_ID_UPDATED = "Юзер с id {} обновлен";
	static String USER_BY_ID_DELETED = "Юзер с id {} удален";
	static String USER_PICTURE_IMAGE_SAVED_WITH_URL = "Новая изображение юзера сохранен по след. url: {}";
	static String USER_BY_ID_FOUND = "Найден юзер с id {}";
	static String USER_ADD_TO_FAVOURITE_POSTS = "Юзер с id {} добавил публикацию с id {} в избранные";
	static String USER_DELETE_FROM_FAVOURITE_POSTS = "Юзер с id {} удалил публикацию с id {} из избранных";
	static String TOTAL_NUMBER_FAVOURITE_POSTS = "У юзера с id {} всего {} избранных публикаций";
	static String TOTAL_NUMBER_CREATED_POSTS = "У юзера с id {} всего {} созданных публикаций";
	static String USER_BY_ID_NOT_FOUND = "Юзер с id {} не найден";
	static String FILE_NOT_VALID_IMAGE_FORMAT = "Изображение не валидного формата";
	static String ERROR_OCCURRED_WHILE_SAVING_IMAGE = "Произошла ошибка при сохранении изображения:";
	static String USER_BY_USERNAME_ALREADY_EXISTS = "Юзер с username \"{}\" уже существует";
	static String IMAGE_WITH_FILENAME_RECEIVED = "Изображение с названием {} получен";
	static String IMAGE_BY_FILENAME_NOT_FOUND = "Изображение с названием {} не найден";
	static String ERROR_OCCURRED_WHILE_RECEIVING_IMAGE = "Произошла ошибка при получении изображения:";

	@Pointcut("within(kg.musabaev.megalabnews.service.UserService+)")
	void targetPackage() {
	}

	@AfterReturning("targetPackage() && execution(* getById(..))")
	void afterReturningMethodGetById(JoinPoint jp) {
		log.debug(USER_BY_ID_FOUND, jp.getArgs()[0]);
	}

	@AfterThrowing(value = "targetPackage() && execution(* getById(..))", throwing = "e")
	void afterThrowingMethodGetById(JoinPoint jp, Exception e) {
		ifUserNotFound(e, () -> log.debug(USER_BY_ID_NOT_FOUND, jp.getArgs()[0]));
	}


	@AfterReturning("targetPackage() && execution(* addToFavouritePosts(..))")
	void afterReturningMethodAddToFavouritePosts(JoinPoint jp) {
		log.debug(USER_ADD_TO_FAVOURITE_POSTS, jp.getArgs()[0], ((AddToFavouritePostsRequest) jp.getArgs()[1]).postId());
	}

	@AfterThrowing(value = "targetPackage() && execution(* addToFavouritePosts(..))", throwing = "e")
	void afterThrowingMethodAddToFavouritePosts(JoinPoint jp, Exception e) {
		Utils.iterateChainOfChecks(e, List.of(
				ifUserNotFound(e, () -> log.debug(USER_BY_ID_NOT_FOUND, jp.getArgs()[0])),
				ifPostNotFound(e, () -> log.debug(POST_BY_ID_NOT_FOUND, jp.getArgs()[1])),
				ifConflict(e, () -> log.debug(POST_ALREADY_EXISTS_IN_FAVOURITES, jp.getArgs()[0])))
		);
	}


	@AfterReturning("targetPackage() && execution(* deleteFromFavouritePosts(..))")
	void afterReturningMethodDeleteFromFavouritePosts(JoinPoint jp) {
		log.debug(USER_DELETE_FROM_FAVOURITE_POSTS, jp.getArgs()[0], jp.getArgs()[1]);
	}

	@AfterThrowing(value = "targetPackage() && execution(* deleteFromFavouritePosts(..))", throwing = "e")
	void afterThrowingMethodDeleteFromFavouritePosts(JoinPoint jp, Exception e) {
		ifUserNotFound(e, () -> log.debug(USER_BY_ID_NOT_FOUND, jp.getArgs()[0]));
	}


	@AfterReturning(value = "targetPackage() && execution(* getAllFavouritePostsByUserId(..))", returning = "r")
	void afterReturningMethodGetAllFavouritePostsByUserId(JoinPoint jp, Page<PostListView> r) {
		log.debug(TOTAL_NUMBER_FAVOURITE_POSTS, jp.getArgs()[0], r.getTotalElements());
	}

	@AfterThrowing(value = "targetPackage() && execution(* getAllFavouritePostsByUserId(..))", throwing = "e")
	void afterThrowingMethodGetAllFavouritePostsByUserId(JoinPoint jp, Exception e) {
		ifUserNotFound(e, () -> log.debug(USER_BY_ID_NOT_FOUND, jp.getArgs()[0]));
	}


	@AfterReturning(value = "targetPackage() && execution(* getAllCreatedPostsByUserId(..))", returning = "r")
	void afterReturningMethodGetAllCreatedPostsByUserId(JoinPoint jp, Page<PostListView> r) {
		log.debug(TOTAL_NUMBER_CREATED_POSTS, jp.getArgs()[0], r.getTotalElements());
	}

	@AfterThrowing(value = "targetPackage() && execution(* getAllCreatedPostsByUserId(..))", throwing = "e")
	void afterThrowingMethodGetAllCreatedPostsByUserId(JoinPoint jp, Exception e) {
		ifUserNotFound(e, () -> log.debug(USER_BY_ID_NOT_FOUND, jp.getArgs()[0]));
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* update(..)))")
	void afterReturningMethodUpdate(JoinPoint jp) {
		log.debug(USER_BY_ID_UPDATED, jp.getArgs()[0]);
	}

	@AfterThrowing(pointcut = "targetPackage() && execution(* update(..))", throwing = "e")
	void afterThrowingMethodUpdate(JoinPoint jp, Exception e) {
		iterateChainOfChecks(e, List.of(
				ifConflict(e, () -> log.debug(USER_BY_USERNAME_ALREADY_EXISTS, ((UpdateUserRequest) jp.getArgs()[1]).username())),
				ifUserNotFound(e, () -> log.debug(USER_BY_ID_NOT_FOUND, jp.getArgs()[0]))
		));
	}


	@AfterReturning(pointcut = "targetPackage() && execution(* uploadUserPicture(..)))", returning = "r")
	void afterReturningMethodUploadUserPicture(String r) {
		log.debug(USER_PICTURE_IMAGE_SAVED_WITH_URL, r);
	}

	@AfterThrowing(pointcut = "targetPackage() && execution(* uploadUserPicture(..))", throwing = "e")
	void afterThrowingMethodUploadUserPicture(Exception e) {
		iterateChainOfChecks(e, List.of(
				ifBadRequest(e, () -> log.debug(FILE_NOT_VALID_IMAGE_FORMAT)),
				ifInternalServerError(e, () -> log.warn(ERROR_OCCURRED_WHILE_SAVING_IMAGE, e))
		));
	}


	@AfterReturning("targetPackage() && execution(* getUserPictureByFilename(..)))")
	void afterReturningMethodGetUserPictureByFilename(JoinPoint jp) {
		log.debug(IMAGE_WITH_FILENAME_RECEIVED, jp.getArgs()[0]);
	}

	@AfterThrowing(pointcut = "targetPackage() && execution(* getUserPictureByFilename(..))", throwing = "e")
	void afterThrowingMethodGetUserPictureByFilename(JoinPoint jp, Exception e) {
		iterateChainOfChecks(e, List.of(
				ifNotFound(e, () -> log.debug(IMAGE_BY_FILENAME_NOT_FOUND, jp.getArgs()[0])),
				ifInternalServerError(e, () -> log.warn(ERROR_OCCURRED_WHILE_RECEIVING_IMAGE, e))
		));
	}


	@AfterReturning("targetPackage() && execution(* deleteById(..))")
	void afterReturningDeleteById(JoinPoint jp) {
		log.debug(USER_BY_ID_DELETED, jp.getArgs()[0]);
	}

	@AfterThrowing(value = "targetPackage() && execution(* deleteById(..))", throwing = "e")
	void afterThrowingMethodDeleteById(JoinPoint jp, Exception e) {
		ifUserNotFound(e, () -> log.debug(USER_BY_ID_NOT_FOUND, jp.getArgs()[0]));
	}
}
