package com.konfigyr.mcp;

import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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
class McpEndpoint implements McpStatelessServerTransport {

	private volatile McpJsonMapper mapper;
	private volatile McpStatelessServerHandler serverHandler;

	@Autowired
	void setMapper(McpJsonMapper mapper) {
		this.mapper = mapper;
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
	ResponseEntity<String> handle(@RequestHeader(HttpHeaders.ACCEPT) String accept, @RequestBody String body) throws IOException {
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

		return switch (message) {
			case McpSchema.JSONRPCRequest request -> handle(request);
			case McpSchema.JSONRPCNotification notification -> handle(notification);
			default -> errorResponse(HttpStatus.BAD_REQUEST, McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
					.message("The server accepts either requests or notifications")
					.build());
		};
	}

	ResponseEntity<String> handle(McpSchema.JSONRPCRequest request) throws IOException {
		McpSchema.JSONRPCResponse response;

		try {
			response = serverHandler.handleRequest(McpTransportContext.EMPTY, request).block();
		} catch (Exception ex) {
			log.error("Failed to handle incoming JSON-RPC request", ex);

			response = McpSchema.JSONRPCResponse.error(request.id(), new McpSchema.JSONRPCResponse.JSONRPCError(
					McpSchema.ErrorCodes.INTERNAL_ERROR, "Unexpected error occurred while handling JSON RPC request"
			));
		}

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(mapper.writeValueAsString(response));
	}

	ResponseEntity<String> handle(McpSchema.JSONRPCNotification notification) throws IOException {
		try {
			serverHandler.handleNotification(McpTransportContext.EMPTY, notification).block();
		} catch (Exception ex) {
			log.error("Failed to handle incoming JSON-RPC notification", ex);

			return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
					.message("Unexpected error occurred while handling JSON RPC notification")
					.build());
		}

		return ResponseEntity.accepted().build();
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
	ResponseEntity<String> errorResponse(HttpStatusCode statusCode, McpError error) throws IOException {
		final Map<String, Object> response = new LinkedHashMap<>(3);
		response.put("jsonrpc", McpSchema.JSONRPC_VERSION);
		response.put("id", null);
		response.put("error", error.getJsonRpcError());

		return ResponseEntity.status(statusCode)
				.contentType(MediaType.APPLICATION_JSON)
				.body(mapper.writeValueAsString(response));
	}

}
