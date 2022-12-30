package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import kg.musabaev.megalabnews.repository.projection.CommentListView;
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

import static kg.musabaev.megalabnews.util.Utils.ifCommentNotFound;
import static kg.musabaev.megalabnews.util.Utils.ifPostNotFound;

@Component
@Aspect
@Log4j2
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SimpleCommentServiceLoggingAspect {

	static String NEW_COMMENT_SAVED = "Новый комментарий: {}";
	static String TOTAL_NUMBER_ROOT_COMMENTS_OF_POST = "У публикации с id {} всего {} корневых комментариев";
	static String TOTAL_NUMBER_CHILD_COMMENTS_OF_PARENT_COMMENT = "У родительского комментария с {} всего {} дочерних комментариев";
	static String COMMENT_BY_ID_UPDATED = "Комментарий с id {} обновлен";
	static String POST_BY_ID_NOT_FOUND = "Публикация с id {} не найден";
	static String COMMENT_BY_ID_NOT_FOUND = "Комментарий с id {} не найден";

	@Pointcut("within(kg.musabaev.megalabnews.service.impl.SimpleCommentService)")
	void targetPackage() {
	}

	@AfterReturning(
			pointcut = "targetPackage() && execution(* save(..)))",
			returning = "r")
	void afterReturningMethodSave(NewOrUpdateCommentResponse r) {
		log.debug(NEW_COMMENT_SAVED, r);
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* save(..)))",
			throwing = "e")
	void afterThrowingMethodSave(JoinPoint jp, Exception e) {
		Utils.iterateChainOfChecks(e, List.of(
				ifPostNotFound(e, () -> log.debug(POST_BY_ID_NOT_FOUND, jp.getArgs()[0])),
				ifCommentNotFound(e, () -> {
					log.debug(COMMENT_BY_ID_NOT_FOUND, ((NewCommentRequest) jp.getArgs()[1]).parentId());
				}))
		);
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* getRootsByPostId(..)))",
			returning = "r")
	void afterReturningMethodGetRootsByPostId(JoinPoint jp, Page<CommentListView> r) {
		log.debug(TOTAL_NUMBER_ROOT_COMMENTS_OF_POST, jp.getArgs()[0], r.getTotalElements());
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* getRootsByPostId(..)))",
			throwing = "e")
	void afterThrowingMethodGetRootsByPostId(JoinPoint jp, Exception e) {
		ifPostNotFound(e, () -> log.debug(POST_BY_ID_NOT_FOUND, jp.getArgs()[0]));
	}

	@AfterReturning(
			pointcut = "targetPackage() && execution(* getChildrenByParentId(..)))",
			returning = "r")
	void afterReturningMethodGetChildrenByParentId(JoinPoint jp, Page<CommentListView> r) {
		log.debug(TOTAL_NUMBER_CHILD_COMMENTS_OF_PARENT_COMMENT, jp.getArgs()[1], r.getTotalElements());
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* getChildrenByParentId(..)))",
			throwing = "e")
	void afterThrowingMethodGetChildrenByParentId(JoinPoint jp, Exception e) {
		Utils.iterateChainOfChecks(e, List.of(
				ifPostNotFound(e, () -> log.debug(POST_BY_ID_NOT_FOUND, jp.getArgs()[0])),
				ifCommentNotFound(e, () -> log.debug(COMMENT_BY_ID_NOT_FOUND, jp.getArgs()[1])))
		);
	}


	@AfterReturning(
			pointcut = "targetPackage() && execution(* update(..)))")
	void afterReturningMethodUpdate(JoinPoint jp) {
		log.debug(COMMENT_BY_ID_UPDATED, jp.getArgs()[1]);
	}

	@AfterThrowing(
			pointcut = "targetPackage() && execution(* update(..)))",
			throwing = "e")
	void afterThrowingMethodUpdate(JoinPoint jp, Exception e) {
		Utils.iterateChainOfChecks(e, List.of(
				ifPostNotFound(e, () -> log.debug(POST_BY_ID_NOT_FOUND, jp.getArgs()[0])),
				ifCommentNotFound(e, () -> log.debug(COMMENT_BY_ID_NOT_FOUND, jp.getArgs()[1])))
		);
	}


	@AfterThrowing(
			pointcut = "targetPackage() && execution(* deleteById(..)))",
			throwing = "e")
	void afterThrowingMethodDeleteById(JoinPoint jp, Exception e) {
		ifCommentNotFound(e, () -> log.debug("Комментарий с id {} не найден", jp.getArgs()[1]));
	}

	@AfterReturning(
			pointcut = "targetPackage() && execution(* deleteById(..)))")
	void afterReturningMethodDeleteById(JoinPoint jp) {
		log.debug(COMMENT_BY_ID_NOT_FOUND, jp.getArgs()[1]);
	}


}
