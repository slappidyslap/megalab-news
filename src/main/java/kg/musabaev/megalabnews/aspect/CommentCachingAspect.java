package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentMap;

@Component
@Aspect
@RequiredArgsConstructor
@Log4j2
public class CommentCachingAspect {

	private final CacheManager cacheManager;

	public static final String childCommentsCacheName = "childCommentList";
	public static final String rootCommentsCacheName = "rootCommentList";

	@Pointcut("within(kg.musabaev.megalabnews.service.impl.SimpleCommentService)")
	void targetPackage() {
	}

	/*@AfterReturning(
			pointcut = "targetPackage() && execution(* getRootsByPostId(..))",
			returning = "page")
	void saveRootCommentsCache(JoinPoint jp, Page<CommentListView> page) {
		Long postId = ((Long) jp.getArgs()[0]);
		Pageable pageable = ((Pageable) jp.getArgs()[1]);

		Map<Long, Map<String, Object>> cacheKey = Map.of(postId, buildMapFromPageableAndPostId(pageable, postId));

		Cache rootCommentsCache = cacheManager.getCache(rootCommentsCacheName);
		Map<Long, Map<String, Object>> map = rootCommentsCache.get(postId, HashMap::new);
		map.get(postId);
	}

	@AfterReturning(
			pointcut = "targetPackage() && execution(* getRootsByPostId(..))",
			returning = "page")
	void saveChildCommentsCache(JoinPoint jp, Page<CommentListView> page) {
		Long postId = ((Long) jp.getArgs()[0]);
		Pageable pageable = ((Pageable) jp.getArgs()[1]);

		Map<String, Object> cacheKey = buildMapFromPageableAndPostId(pageable, postId);

		cacheManager.getCache(rootCommentsCacheName).put(cacheKey, page);
	}

	private Map<String, Object> buildMapFromPageableAndPostId(Pageable pageable, Long postId) {
		int pageNumber = pageable.getPageNumber();
		int pageSize = pageable.getPageSize();

		Map<String, String> sort = new HashMap<>();
		for (Sort.Order order : pageable.getSort())
			sort.put(order.getProperty(), order.getDirection().toString());

		return Map.of(
				"postId", postId,
				"pageable", Map.of(
						"pageNumber", pageNumber,
						"pageSize", pageSize,
						"sort", sort
				)
		);
	}

	@Around("targetPackage() && execution(* getRootsByPostId(..))")
	Page<CommentListView> saveRootCommentsCache(ProceedingJoinPoint pjp) {


		return null;
	}*/

	@AfterReturning("targetPackage() && execution(* save(..))")
	void onSaveCommentDeleteCache(JoinPoint jp) {
		long postId = ((long) jp.getArgs()[0]);
		var dto = (NewCommentRequest) jp.getArgs()[1];

		if (dto.parentId() == null) {
			ConcurrentMap<Object, Object> store = (ConcurrentMap<Object, Object>) cacheManager
					.getCache(rootCommentsCacheName)
					.getNativeCache();
			store.entrySet().removeIf(entry -> {
				/*long cachedPostId = ((long) ((SimpleKey) ((AbstractMap.SimpleImmutableEntry) entry)
						.getKey())
						.params[0]);
				return cachedPostId == postId;*/
				return true;
			});
			log.debug("Удалены все данные у кэша {} по ключу postId: {}", rootCommentsCacheName, postId);
		} /*else {
			ConcurrentMap<Object, Object> store = (ConcurrentMap<Object, Object>) cacheManager.getCache(rootCommentsCacheName).getNativeCache();
			store.entrySet().removeIf(entry -> {
			});
		}*/
	}
}
