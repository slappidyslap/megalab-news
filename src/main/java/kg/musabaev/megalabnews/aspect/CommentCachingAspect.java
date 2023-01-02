package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.dto.NewCommentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
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

	@AfterReturning("targetPackage() && execution(* save(..))")
	void onSaveCommentDeleteCache(JoinPoint jp) {
		Long postId = ((long) jp.getArgs()[0]);
		var dto = (NewCommentRequest) jp.getArgs()[1];

		if (dto.parentId() == null) {
			ConcurrentMap<Object, Object> store = (ConcurrentMap<Object, Object>) cacheManager
					.getCache(rootCommentsCacheName)
					.getNativeCache();

			store.entrySet().removeIf(entry -> {
				Pair<Object, Object> pair = (Pair<Object, Object>) entry.getKey(); // key

				boolean isPostIdEquals = pair.getKey().equals(postId);
				if (isPostIdEquals) log.debug("Удалено значение у кэша {} по ключу {}", rootCommentsCacheName, pair);
				return isPostIdEquals;
			});
		} /*else {
			ConcurrentMap<Object, Object> store = (ConcurrentMap<Object, Object>) cacheManager.getCache(rootCommentsCacheName).getNativeCache();
			store.entrySet().removeIf(entry -> {
			});
		}*/
	}
}
