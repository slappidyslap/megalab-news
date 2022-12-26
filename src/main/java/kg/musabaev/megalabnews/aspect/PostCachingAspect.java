package kg.musabaev.megalabnews.aspect;

import kg.musabaev.megalabnews.dto.NewOrUpdatePostResponse;
import kg.musabaev.megalabnews.mapper.PostDtoPostModelMapper;
import kg.musabaev.megalabnews.model.Post;
import kg.musabaev.megalabnews.service.impl.SimplePostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Aspect
@RequiredArgsConstructor
@Log4j2
public class PostCachingAspect {

	private final CacheManager cacheManager;
	private final PostDtoPostModelMapper mapper;

	@AfterReturning(
			pointcut = "execution(" +
					"public * kg.musabaev.megalabnews.service.impl.SimplePostService.update(..))",
			returning = "responseDto")
	void updateCachePostItem(JoinPoint joinPoint, NewOrUpdatePostResponse responseDto) {
		Long postId = (Long) joinPoint.getArgs()[0];
		String cacheName = SimplePostService.postItemCacheName;

		Cache cachePostItem = cacheManager.getCache(cacheName);
		if (Objects.isNull(cachePostItem)) return;
		Post cachedPost = cachePostItem.get(postId, Post.class);
		if (Objects.isNull(cachedPost)) return;

		mapper.updatePostModelByPostDto(responseDto, cachedPost);

		cachePostItem.put(postId, cachedPost);

		log.debug("Обновлены данные у кеша {} с ключом {}", cacheName, postId);
	}
}
