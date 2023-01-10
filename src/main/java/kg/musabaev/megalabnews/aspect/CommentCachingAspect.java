package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.dto.NewOrUpdateCommentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentMap;

import static kg.musabaev.megalabnews.service.impl.SimpleCommentService.childCommentsCacheName;
import static kg.musabaev.megalabnews.service.impl.SimpleCommentService.rootCommentsCacheName;

@Component
@Aspect
@RequiredArgsConstructor
@Log4j2
@ConditionalOnExpression("${app.cache-enabled} == true")
public class CommentCachingAspect {

	public static final String CACHE_DELETED_BY_KEY = "Удалено значение у кэша {} по ключу {}";

	private final CacheManager cacheManager;

	@Pointcut("within(kg.musabaev.megalabnews.service.impl.SimpleCommentService)")
	void targetPackage() {
	}

	@AfterReturning(
			value = "targetPackage() && execution(* save(..)) ||" +
					"targetPackage() && execution(* update(..))",
			returning = "dto")
	void onSaveCommentDeleteCache(NewOrUpdateCommentResponse dto) {
		Long postId = dto.postId();
		Long parentId = dto.parentId();

		if (parentId == null) deleteRootCommentsCacheByPostId(postId);
		else deleteChildCommentsCacheByPostIdAndParentCommentId(postId, parentId);
	}

	@AfterReturning("targetPackage() && execution(* deleteById(..)) ||" +
			"execution(* kg.musabaev.megalabnews.service.impl.SimplePostService.deleteById(..))")
	void onUpdateAndDeleteCommentDeleteCache(JoinPoint jp) {
		Long postId = (Long) jp.getArgs()[0];
		deleteRootCommentsCacheByPostId(postId);
		deleteChildCommentsCacheByPostIdAndAnyCommentId(postId);
	}

	private void deleteRootCommentsCacheByPostId(Long postId) {
		ConcurrentMap<Object, Object> store = getStoreFromCacheManager(rootCommentsCacheName);

		store.entrySet().removeIf(entry -> {
			var pair = (Pair<Long, Pageable>) entry.getKey();

			boolean isEquals = pair.getKey().equals(postId);
			if (isEquals) log.debug(CACHE_DELETED_BY_KEY, rootCommentsCacheName, pair);
			return isEquals;
		});
	}

	private void deleteChildCommentsCacheByPostIdAndParentCommentId(Long postId, Long parentId) {
		ConcurrentMap<Object, Object> store = getStoreFromCacheManager(childCommentsCacheName);

		store.entrySet().removeIf(entry -> {
			var triple = (Triple<Long, Long, Pageable>) entry.getKey();

			boolean isEquals = triple.getLeft().equals(postId) && triple.getMiddle().equals(parentId);
			if (isEquals) log.debug(CACHE_DELETED_BY_KEY, childCommentsCacheName, triple);
			return isEquals;
		});
	}

	private void deleteChildCommentsCacheByPostIdAndAnyCommentId(Long postId) {
		ConcurrentMap<Object, Object> store = getStoreFromCacheManager(childCommentsCacheName);

		store.entrySet().removeIf(entry -> {
			var triple = (Triple<Long, Long, Pageable>) entry.getKey();

			boolean isEquals = triple.getLeft().equals(postId);
			if (isEquals) log.debug(CACHE_DELETED_BY_KEY, childCommentsCacheName, triple);
			return isEquals;
		});
	}

	/**
	 * @see ConcurrentMapCache#getNativeCache()
	 */
	private ConcurrentMap<Object, Object> getStoreFromCacheManager(String cacheName) {
		return (ConcurrentMap<Object, Object>) cacheManager
				.getCache(cacheName)
				.getNativeCache();
	}
}
