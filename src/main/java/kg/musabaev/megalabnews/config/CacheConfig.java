package kg.musabaev.megalabnews.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
