package kg.musabaev.megalabnews.config;

import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

@Component
public class CommentCacheKeyGenerator extends SimpleKeyGenerator {

	@Override
	public Object generate(Object target, Method method, Object... params) {
		return super.generate(target, method, Map.of(params[0], params[1]));
	}
}
