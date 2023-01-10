package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.mapper.PostMapper;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.service.impl.SimplePostService;
import kg.musabaev.megalabnews.service.impl.SimpleUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.awt.print.Pageable;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

import static kg.musabaev.megalabnews.aspect.CommentCachingAspect.CACHE_DELETED_BY_KEY;
import static kg.musabaev.megalabnews.service.impl.SimpleCommentService.rootCommentsCacheName;

@Component
@Aspect
@RequiredArgsConstructor
@Log4j2
@ConditionalOnExpression("${app.cache-enabled} == true")
public class PostCachingAspect {

	private final CacheManager cacheManager;
	private final PostMapper mapper;

	@Pointcut("within(kg.musabaev.megalabnews.service.impl.SimplePostService)")
	void targetPackage() {
	}

	@AfterReturning(
			pointcut = "targetPackage() && execution(* update(..))",
			returning = "responseDto")
	void updateCachePostItem(JoinPoint joinPoint, NewOrUpdatePostResponse responseDto) {
		Long postId = (Long) joinPoint.getArgs()[0];
		String cacheName = SimplePostService.postItemCacheName;

		Cache cachePostItem = cacheManager.getCache(cacheName);
		if (Objects.isNull(cachePostItem)) return;
		Post cachedPost = cachePostItem.get(postId, Post.class);
		if (Objects.isNull(cachedPost)) return;

		mapper.update(responseDto, cachedPost);

		cachePostItem.put(postId, cachedPost);

		log.debug("Обновлены данные у кэша {} с ключом {}", cacheName, postId);
	}

	@AfterReturning(
			pointcut = "targetPackage() && execution(* update(..)) || " +
					"targetPackage() && execution(* save(..))",
			returning = "r")
	void deleteUserCreatedPostsOnUpdatingPost(NewOrUpdatePostResponse r) {
		ConcurrentMap<Object, Object> store = getStoreFromCacheManager(SimpleUserService.USER_CREATED_POSTS_CACHE_NAME);

		store.entrySet().removeIf(entry -> {
			Pair<Long, Pageable> key = (Pair<Long, Pageable>) entry.getKey();

			boolean isEquals = key.getKey().equals(r.author().id());
			if (isEquals) log.debug(CACHE_DELETED_BY_KEY, rootCommentsCacheName, key);
			return isEquals;
		});
	}

	private ConcurrentMap<Object, Object> getStoreFromCacheManager(String cacheName) {
		return (ConcurrentMap<Object, Object>) cacheManager
				.getCache(cacheName)
				.getNativeCache();
	}
}
