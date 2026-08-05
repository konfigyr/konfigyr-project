package com.konfigyr.mcp.invoke;

import io.modelcontextprotocol.common.McpTransportContext;
import org.jspecify.annotations.NullMarked;
import reactor.core.publisher.Mono;

/**
 * Handles one incoming MCP request for a specific construct producing that construct's result.
 * The construct might be a resource read, a tool call, or any future annotation-driven MCP construct.
 *
 * @param <T> the request type
 * @param <R> the result type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public interface McpInvoker<T, R> {

	/**
	 * Handles {@code request} and produces its result.
	 *
	 * @param context the transport context the request arrived on, can't be {@literal null}
	 * @param request the incoming request, can't be {@literal null}
	 * @return the result, never {@literal null}
	 */
	Mono<R> invoke(McpTransportContext context, T request);

}
