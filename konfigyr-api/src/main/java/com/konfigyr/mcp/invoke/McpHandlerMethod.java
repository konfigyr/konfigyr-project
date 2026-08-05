package com.konfigyr.mcp.invoke;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Reflectively invokes a {@link Method} on behalf of an MCP construct - a resource, a tool, or
 * any future annotation-driven handler.
 * <p>
 * Each parameter is resolved from one of two sources. A set of context objects - e.g., the
 * transport context, or the incoming request - is checked first, matching by type. Anything left
 * unmatched falls back to a named raw argument instead: a URI template variable for a resource, a
 * tool call argument for a tool. That raw value is then converted to the parameter's declared
 * type by the {@link McpParameterTypeConverter} given at construction.
 * <p>
 * A parameter's binding name and annotations are resolved once, up front, by
 * {@link McpHandlerMethodFactory} - this class only consumes the already-resolved
 * {@link McpHandlerParameter} list, which it also exposes via {@link Iterable}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see McpHandlerMethodFactory
 */
@NullMarked
public final class McpHandlerMethod implements Iterable<McpHandlerParameter> {

	private final Object bean;
	private final Method method;
	private final McpParameterTypeConverter converter;
	private final List<McpHandlerParameter> parameters;

	/**
	 * @param bean the bean instance {@code method} will be invoked on, can't be {@literal null}
	 * @param method the handler method, can't be {@literal null}
	 * @param converter converter used to bind raw argument values to each parameter's declared
	 * type, can't be {@literal null}
	 * @param parameters {@code method}'s already-resolved parameters, can't be {@literal null}
	 */
	McpHandlerMethod(Object bean, Method method, McpParameterTypeConverter converter, List<McpHandlerParameter> parameters) {
		this.bean = bean;
		this.method = method;
		this.converter = converter;
		this.parameters = Collections.unmodifiableList(parameters);
		ReflectionUtils.makeAccessible(method);
	}

	/**
	 * Returns the list of method parameters that were detected on this MCP handler method.
	 *
	 * @return handler method parameters, never {@literal null}
	 */
	public List<McpHandlerParameter> parameters() {
		return parameters;
	}

	/**
	 * Returns the return type of the MCP handler method.
	 *
	 * @return the return type, never {@literal null}
	 */
	public ResolvableType returnType() {
		return ResolvableType.forMethodReturnType(method);
	}

	/**
	 * Invokes the target method, resolving its parameters from {@code context} (matched by
	 * assignability, in declaration order) and {@code arguments} (matched by name, then converted
	 * to the parameter's declared type).
	 *
	 * @param arguments named raw argument values, e.g. URI template variables or tool call
	 * arguments, can't be {@literal null}
	 * @param context candidate objects bound to any parameter their type is assignable from, e.g.
	 * the transport context or the incoming request, can't be {@literal null}
	 * @return whatever the target method returns, may be {@literal null}
	 */
	@Nullable
	public Object invoke(Map<String, ?> arguments, Object... context) {
		final Object[] args = new Object[parameters.size()];

		for (int i = 0; i < parameters.size(); i++) {
			args[i] = resolveArgument(parameters.get(i), arguments, context);
		}

		return ReflectionUtils.invokeMethod(method, bean, args);
	}

	/**
	 * Iterates over {@link #parameters()}, in declaration order.
	 *
	 * @return an iterator over the handler method parameters, never {@literal null}
	 */
	@Override
	public Iterator<McpHandlerParameter> iterator() {
		return parameters().iterator();
	}

	@Nullable
	private Object resolveArgument(McpHandlerParameter parameter, Map<String, ?> arguments, Object[] context) {
		for (Object candidate : context) {
			if (parameter.type().isInstance(candidate)) {
				return candidate;
			}
		}

		return converter.convert(parameter, arguments.get(parameter.name()));
	}

}
