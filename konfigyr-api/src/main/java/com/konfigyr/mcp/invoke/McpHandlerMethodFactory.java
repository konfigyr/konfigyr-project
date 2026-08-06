package com.konfigyr.mcp.invoke;

import com.konfigyr.mcp.annotation.McpParameter;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodClassKey;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Creates and caches {@link McpHandlerMethod} instances, keyed by the method and the bean's
 * target class.
 * <p>
 * Building a {@link McpHandlerMethod} means resolving every parameter's binding name and
 * annotations up front - work that only needs to happen once per handler method, not on every
 * invocation, and not once per MCP construct that happens to share the same handler bean.
 * <p>
 * A parameter's binding name comes from any annotation meta-annotated with {@link McpParameter}
 * declared on it, or its own compiled-in name otherwise - resolving that compiled-in name
 * requires compiling with {@code -parameters}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public final class McpHandlerMethodFactory {

	private final Map<MethodClassKey, McpHandlerMethod> cache;
	private final McpParameterTypeConverter converter;
	private final ParameterNameDiscoverer discoverer;

	/**
	 * Creates a new {@link McpHandlerMethodFactory} with the given {@link McpJsonMapper} and
	 * {@link ConversionService} factory provider.
	 *
	 * @param jsonMapper mapper used both to convert values that a {@link ConversionService}
	 *                   can't handle, and by every {@link McpHandlerMethod} this factory creates,
	 *                   can't be {@literal null}
	 * @param conversionService lazily resolved conversion service tried first when converting a
	 *                          raw argument value, can't be {@literal null}
	 */
	public McpHandlerMethodFactory(McpJsonMapper jsonMapper, Supplier<ConversionService> conversionService) {
		this.converter = new McpParameterTypeConverter(conversionService, jsonMapper);
		this.discoverer = new DefaultParameterNameDiscoverer();
		this.cache = new ConcurrentHashMap<>(16);
	}

	/**
	 * Returns the {@link McpHandlerMethod} for the given bean and method, creating and caching
	 * one the first time this exact method/target-class pair is requested.
	 *
	 * @param bean the bean instance the method will be invoked on, can't be {@literal null}
	 * @param method the handler method, can't be {@literal null}
	 * @return the (possibly cached) handler method, never {@literal null}
	 */
	public McpHandlerMethod create(Object bean, Method method) {
		return cache.computeIfAbsent(new MethodClassKey(method, AopUtils.getTargetClass(bean)), ignore ->
				new McpHandlerMethod(bean, method, converter, createParameters(method, discoverer)));
	}

	/**
	 * Builds the {@link McpHandlerParameter} list for every parameter declared by {@code method},
	 * resolving each one's binding name along the way.
	 *
	 * @param method the method to inspect, can't be {@literal null}
	 * @param discoverer the parameter name discoverer to use, can't be {@literal null}
	 * @return the list of MCP method handler parameters, never {@literal null}
	 */
	private static List<McpHandlerParameter> createParameters(Method method, ParameterNameDiscoverer discoverer) {
		final int size = method.getParameterCount();
		final List<McpHandlerParameter> bindings = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			final MethodParameter parameter = new MethodParameter(method, i);
			parameter.initParameterNameDiscovery(discoverer);

			final MergedAnnotations annotations = MergedAnnotations.from(parameter.getParameterAnnotations());
			bindings.add(McpHandlerParameter.of(resolveName(parameter, annotations), annotations, parameter));
		}

		return bindings;
	}

	/**
	 * Resolves a parameter's binding name: the {@link McpParameter#name()} of any annotation
	 * meta-annotated with {@link McpParameter} declared on it, if present and non-blank, or its
	 * own compiled-in name otherwise.
	 *
	 * @param parameter the parameter to resolve, can't be {@literal null}
	 * @param annotations the annotations declared on the parameter, can't be {@literal null}
	 * @return the resolved parameter name, never {@literal null}
	 */
	private static String resolveName(MethodParameter parameter, MergedAnnotations annotations) {
		final MergedAnnotation<McpParameter> annotation = annotations.get(McpParameter.class);

		String name = null;

		if (annotation.isPresent()) {
			name = annotation.getString("name");
		}

		if (StringUtils.isBlank(name)) {
			name = parameter.getParameterName();
		}

		Assert.state(name != null, () -> "Could not resolve a name for parameter: " + parameter);

		return name;
	}

}
