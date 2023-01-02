package kg.musabaev.megalabnews.config;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

@Configuration
@EnableCaching
@Log4j2
public class CacheConfig {

	@Bean
	@ConditionalOnExpression("${app.cache-enabled} == true")
	public CacheManager concurrentMapCacheManager() {
		log.debug("{} используется как реализация {}", ConcurrentMapCacheManager.class, CacheManager.class);

		return new ConcurrentMapCacheManager();
	}

	@Bean
	@ConditionalOnExpression("${app.cache-enabled} == false")
	public CacheManager noOpCacheManager() {
		log.debug("{} используется как реализация {}", NoOpCacheManager.class, CacheManager.class);

		return new NoOpCacheManager();
	}

	@Bean
	public KeyGenerator rootCommentCacheKeyGenerator() {
		return new RootCommentCacheKeyGenerator();
	}

	@Bean
	public KeyGenerator childCommentCacheKeyGenerator() {
		return new ChildCommentCacheKeyGenerator();
	}

	private static class RootCommentCacheKeyGenerator extends SimpleKeyGenerator {
		@Override
		public Object generate(Object target, Method method, Object... params) {
			return super.generate(target, method, Pair.of(params[0], params[1]));
		}
	}

	private static class ChildCommentCacheKeyGenerator extends SimpleKeyGenerator {
		@Override
		public Object generate(Object target, Method method, Object... params) {
			return super.generate(target, method, Triple.of(params[0], params[1], params[2]));
		}
	}
}
