package com.konfigyr.mcp;

import com.konfigyr.entity.EntityId;
import com.konfigyr.mcp.annotation.ConditionalOnMcpServer;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.KonfigyrClaimNames;
import com.konfigyr.security.NamespacedPrincipal;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Exposes the Konfigyr MCP server over a stateless {@code /mcp} endpoint as a regular Spring MVC
 * controller, so it is authenticated and authorized the same way as every other REST endpoint.
 * <p>
 * This is a minimal, Spring-native implementation of {@link McpStatelessServerTransport}, since none
 * of MCP SDK implementation avoids a dependency on {@code org.springframework.ai} or compose with this
 * codebase's Spring MVC based security and testing infrastructure at the same time.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@RestController
@ConditionalOnMcpServer
class McpEndpoint implements McpStatelessServerTransport {

	private volatile McpJsonMapper mapper;
	private volatile NamespaceManager namespaces;
	private volatile McpStatelessServerHandler serverHandler;

	@Autowired
	void setMapper(McpJsonMapper mapper) {
		this.mapper = mapper;
	}

	@Autowired
	public void setNamespaceManager(NamespaceManager namespaceManager) {
		this.namespaces = namespaceManager;
	}

	@Override
	public void setMcpHandler(McpStatelessServerHandler handler) {
		this.serverHandler = handler;
	}

	@Override
	public Mono<Void> closeGracefully() {
		return Mono.empty();
	}

	@RequiresScope(OAuthScope.MCP)
	@PostMapping(value = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE)
	Mono<ResponseEntity<String>> handle(
			Authentication authentication,
			@RequestHeader(HttpHeaders.ACCEPT) String accept,
			@RequestBody String body
	) {
		if (!accept.contains(MediaType.APPLICATION_JSON_VALUE) || !accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
			return errorResponse(HttpStatus.BAD_REQUEST, McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
					.message("Both 'application/json' and 'text/event-stream' required in the 'Accept' header")
					.build());
		}

		final McpSchema.JSONRPCMessage message;

		try {
			message = McpSchema.deserializeJsonRpcMessage(mapper, body);
		} catch (IOException | IllegalArgumentException ex) {
			log.error("Failed to deserialize incoming MCP JSON-RPC message", ex);

			return errorResponse(HttpStatus.BAD_REQUEST, McpError.builder(McpSchema.ErrorCodes.PARSE_ERROR)
					.message("Could not parse incoming MCP JSON-RPC message")
					.build());
		} catch (Exception ex) {
			log.error("Unexpected error while deserializing incoming MCP JSON-RPC message", ex);

			return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
					.message("Unexpected error occurred while handling MCP request")
					.build());
		}

		final McpTransportContext context = createTransportContext(authentication);

		return switch (message) {
			case McpSchema.JSONRPCRequest request -> handle(context, request);
			case McpSchema.JSONRPCNotification notification -> handle(context, notification);
			default -> errorResponse(HttpStatus.BAD_REQUEST, McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
					.message("The server accepts either requests or notifications")
					.build());
		};
	}

	Mono<ResponseEntity<String>> handle(McpTransportContext context, McpSchema.JSONRPCRequest request) {
		return serverHandler.handleRequest(context, request)
				.transform(decorate(context))
				.onErrorResume(ex -> {
					log.error("Failed to handle incoming JSON-RPC request", ex);

					final McpError error = McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
							.message("Unexpected error occurred while handling JSON RPC request")
							.build();

					return Mono.just(McpSchema.JSONRPCResponse.error(request.id(), error.getJsonRpcError()));
				})
				.flatMap(response -> createResponse(resolveStatusCode(response), response));
	}

	Mono<ResponseEntity<String>> handle(McpTransportContext context, McpSchema.JSONRPCNotification notification) {
		return serverHandler.handleNotification(context, notification)
				.transform(decorate(context))
				.<ResponseEntity<String>>thenReturn(ResponseEntity.accepted().build())
				.onErrorResume(ex -> {
					log.error("Failed to handle incoming JSON-RPC notification", ex);

					final McpError error = McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
							.message("Unexpected error occurred while handling JSON RPC notification")
							.build();

					return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, error);
				});
	}

	McpTransportContext createTransportContext(Authentication authentication) {
		final AuthenticatedPrincipal principal = AuthenticatedPrincipal.fromAuthentication(authentication)
				.orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
						"Could not resolve authenticated principal from current authentication context"
				));

		final Namespace namespace;

		if (principal instanceof NamespacedPrincipal namespaced) {
			final EntityId id = namespaced.getNamespaceId()
					.orElseThrow(() -> new AccessDeniedException("Authenticated principal is missing its namespace claim"));

			namespace = namespaces.findById(id)
					.orElseThrow(() -> new AccessDeniedException("Authenticated principal's namespace claim does not match a known namespace"));
		} else {
			throw new AccessDeniedException("Authenticated principal is not scoped to a namespace");
		}

		return McpTransportContext.create(Map.of(KonfigyrClaimNames.NAMESPACE, namespace));
	}

	/**
	 * Builds a JSON RPC error response for failures that occur before a request id is known. A bad
	 * {@code Accept} header, or a body that isn't valid JSON or doesn't match any known message shape,
	 * or that can never have one, such as a notification.
	 * <p>
	 * Per the JSON-RPC 2.0 spec, {@code id} MUST be {@literal null} when it can't be determined.
	 * {@link McpSchema.JSONRPCResponse} can't express that itself since its constructor rejects a
	 * {@literal null} id, so this response is built by hand instead of going through
	 * {@link McpSchema.JSONRPCResponse#error(Object, McpSchema.JSONRPCResponse.JSONRPCError)}.
	 *
	 * @param statusCode the HTTP status code to respond with, can't be {@literal null}
	 * @param error the JSON RPC error to report, can't be {@literal null}
	 * @return the JSON RPC error response, never {@literal null}
	 */
	Mono<ResponseEntity<String>> errorResponse(HttpStatusCode statusCode, McpError error) {
		final Map<String, Object> response = new LinkedHashMap<>(3);
		response.put("jsonrpc", McpSchema.JSONRPC_VERSION);
		response.put("id", null);
		response.put("error", error.getJsonRpcError());

		return createResponse(statusCode, response);
	}

	Mono<ResponseEntity<String>> createResponse(HttpStatusCode statusCode, Object body) {
		return Mono.create(sink -> {
			try {
				final ResponseEntity<String> response = ResponseEntity.status(statusCode)
						.contentType(MediaType.APPLICATION_JSON)
						.body(mapper.writeValueAsString(body));

				sink.success(response);
			} catch (IOException ex) {
				sink.error(ex);
			}
		});
	}

	/**
	 * Resolves the HTTP status for a completed JSON-RPC response. Per JSON-RPC/MCP transport
	 * convention, a well-formed response is always {@code 200}, even when it carries a
	 * protocol-level error (method/tool not found, invalid params, ...) - only {@link
	 * McpSchema.ErrorCodes#INTERNAL_ERROR} indicates that handling the request failed
	 * unexpectedly, which is reported as {@code 500}.
	 *
	 * @param response the completed JSON-RPC response, can be {@literal null}
	 * @return the HTTP status code to respond with, never {@literal null}
	 */
	static HttpStatusCode resolveStatusCode(McpSchema.@Nullable JSONRPCResponse response) {
		final boolean internalError = response != null && response.error() != null
				&& response.error().code() == McpSchema.ErrorCodes.INTERNAL_ERROR;

		return internalError ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK;
	}

	static <T> Function<Mono<T>, Publisher<T>> decorate(McpTransportContext context) {
		return original -> original
				.contextWrite(ctx -> ctx.put(McpTransportContext.KEY, context));
	}

}
