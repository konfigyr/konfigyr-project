package com.konfigyr.mcp.invoke;

import io.modelcontextprotocol.common.McpTransportContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Base {@link McpInvoker} that wires a {@link McpHandlerMethod} into one MCP construct's request
 * and result types.
 * <p>
 * {@link #invoke(McpTransportContext, Object)} handles what every construct needs regardless of
 * type: turning the incoming request into named arguments, invoking the handler method with
 * those arguments (plus the transport context and request as candidate context objects), and
 * unwrapping the handler method's return value if it's itself reactive.
 * <p>
 * Subclasses only need to supply {@link #constructArguments(McpTransportContext, Object)} - how
 * named arguments come out of this construct's request type - and
 * {@link #convert(McpTransportContext, Object, Object)} - how the handler method's return value
 * becomes this construct's result type.
 *
 * @param <T> the request type
 * @param <R> the result type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public abstract class AbstractMcpHandlerMethodInvoker<T, R> implements McpInvoker<T, R> {

	protected final McpHandlerMethod method;

	protected AbstractMcpHandlerMethodInvoker(McpHandlerMethod method) {
		this.method = method;
	}

	@Override
	public Mono<R> invoke(McpTransportContext context, T request) {
		return Mono.defer(() -> {
			final Map<String, ?> arguments = constructArguments(context, request);

			final Object result = method.invoke(arguments, context, request);

			if (result instanceof Publisher<?> publisher) {
				return Mono.from(publisher).flatMap(it -> convert(context, request, it));
			}

			return convert(context, request, result);
		});
	}

	/**
	 * Extracts the named raw arguments to bind the handler method's parameters from, out of the
	 * incoming request.
	 *
	 * @param context the transport context the request arrived on, can't be {@literal null}
	 * @param request the incoming request, can't be {@literal null}
	 * @return the named raw arguments, never {@literal null}
	 */
	protected abstract Map<String, ?> constructArguments(McpTransportContext context, T request);

	/**
	 * Converts the handler method's return value into this construct's result type.
	 *
	 * @param context the transport context the request arrived on, can't be {@literal null}
	 * @param request the incoming request, can't be {@literal null}
	 * @param result whatever the handler method returned, may be {@literal null}
	 * @return the converted result, never {@literal null}
	 */
	protected abstract Mono<R> convert(McpTransportContext context, T request, @Nullable Object result);

}
