package kg.musabaev.megalabnews.config;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class RootCommentCacheKeyGenerator extends SimpleKeyGenerator {

	@Override
	public Object generate(Object target, Method method, Object... params) {
		return super.generate(target, method, Pair.of(params[0], params[1]));
	}
}
