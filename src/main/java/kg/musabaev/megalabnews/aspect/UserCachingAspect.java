package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.service.impl.SimpleUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.awt.print.Pageable;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import static kg.musabaev.megalabnews.service.impl.SimpleCommentService.ROOT_COMMENTS_CACHE_NAME;

@Component
@Aspect
@RequiredArgsConstructor
@Log4j2
@ConditionalOnExpression("${app.cache-enabled} == true")
public class UserCachingAspect {

	public static final String CACHE_DELETED_BY_KEY = "Удалено значение у кэша {} по ключу {}";

	private final CacheManager cacheManager;

	@Pointcut("within(kg.musabaev.megalabnews.service.impl.SimpleUserService)")
	void targetPackage() {
	}

	@AfterReturning("targetPackage() && execution(* addToFavouritePosts(..)) ||" +
			"targetPackage() && execution(* deleteFromFavouritePosts(..)) ||" +
			"targetPackage() && execution(* deleteById(..))")
	void deleteFavouriteCacheByUserIdOnAddingOrDeleting(JoinPoint jp) {
		ConcurrentMap<Object, Object> favouritePostsStore
				= getStoreFromCacheManager(SimpleUserService.USER_FAVOURITE_POSTS_CACHE_NAME);
		ConcurrentMap<Object, Object> createdPostsStore
				= getStoreFromCacheManager(SimpleUserService.USER_CREATED_POSTS_CACHE_NAME);

		Predicate<Map.Entry<Object, Object>> predicate = entry -> {
			Pair<Long, Pageable> key = (Pair<Long, Pageable>) entry.getKey();

			boolean isEquals = key.getKey().equals(jp.getArgs()[0]);
			if (isEquals) log.debug(CACHE_DELETED_BY_KEY, ROOT_COMMENTS_CACHE_NAME, key);
			return isEquals;
		};

		favouritePostsStore.entrySet().removeIf(predicate);
		createdPostsStore.entrySet().removeIf(predicate);
	}

	private ConcurrentMap<Object, Object> getStoreFromCacheManager(String cacheName) {
		return (ConcurrentMap<Object, Object>) cacheManager
				.getCache(cacheName)
				.getNativeCache();
	}
}
