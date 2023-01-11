package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.exception.ResponseStatusUnauthorizedException;
import kg.musabaev.megalabnews.repository.CommentRepo;
import kg.musabaev.megalabnews.repository.PostRepo;
import kg.musabaev.megalabnews.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import static kg.musabaev.megalabnews.util.Utils.isAuthenticatedUser;

@Component
@Aspect
@RequiredArgsConstructor
@Log4j2
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthenticationAspect {

	CommentRepo commentRepo;
	PostRepo postRepo;
	UserRepo userRepo;

	@Pointcut("within(kg.musabaev.megalabnews.service.CommentService+)")
	void commentService() {
	}

	@Pointcut("within(kg.musabaev.megalabnews.service.PostService+)")
	void postService() {
	}

	@Pointcut("within(kg.musabaev.megalabnews.service.UserService+)")
	void userService() {
	}

	@Before("commentService() && execution(* update(..)) ||" +
			"commentService() && execution(* deleteById(..))")
	void beforeMethodInCommentServiceVerifyAuthUserIsAuthor(JoinPoint jp) {
		if (!isAuthenticatedUser(
				commentRepo.findAuthorUsernameByPostIdAndCommentId((Long) jp.getArgs()[0], (Long) jp.getArgs()[1])))
			throw new ResponseStatusUnauthorizedException();
	}

	@Before("postService() && execution(* deleteById(..)) ||" +
			"postService() && execution(* update(..))")
	void beforeMethodInPostServiceVerifyAuthUserIsAuthor(JoinPoint jp) {
		if (!isAuthenticatedUser(postRepo.findAuthorUsernameByPostId((Long) jp.getArgs()[0])))
			throw new ResponseStatusUnauthorizedException();
	}

	@Before("userService() && execution(* addToFavouritePosts(..)) ||" +
			"userService() && execution(* deleteFromFavouritePosts(..)) ||" +
			"userService() && execution(* getAllFavouritePostsByUserId(..)) ||" +
			"userService() && execution(* update(..)) ||" +
			"userService() && execution(* deleteById(..))")
	void beforeMethodInUserServiceVerifyAuthUserIsUser(JoinPoint jp) {
		if (!isAuthenticatedUser(userRepo.findUsernameByUserId((Long) jp.getArgs()[0])))
			throw new ResponseStatusUnauthorizedException();
	}
}
