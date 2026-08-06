package com.konfigyr.mcp.tool;

import com.konfigyr.mcp.invoke.AbstractMcpHandlerMethodInvoker;
import com.konfigyr.mcp.invoke.McpHandlerMethod;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link AbstractMcpHandlerMethodInvoker} for {@code tools/call} requests.
 * <p>
 * Uses the request's {@code arguments} map directly as the handler method's named arguments.
 * Converts its return value into a {@link CallToolResult}: an {@link McpSchema.Content} or
 * {@link CharSequence} is wrapped as-is, a {@link StructuredOutput} is serialized to both its
 * JSON text and structured content representations, and anything else is serialized to JSON
 * text only.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
final class McpToolInvoker extends AbstractMcpHandlerMethodInvoker<CallToolRequest, CallToolResult> {

	static final TypeRef<Map<String, Object>> OBJECT_MAP_TYPE = new TypeRef<>() { /**/ };

	static CallToolResult EMPTY_RESULT = CallToolResult
			.builder(List.of())
			.build();

	private final McpJsonMapper mapper;

	McpToolInvoker(McpHandlerMethod method, McpJsonMapper jsonMapper) {
		super(method);
		this.mapper = jsonMapper;
	}

	@Override
	protected Map<String, ?> constructArguments(McpTransportContext context, CallToolRequest request) {
		Map<String, Object> arguments = request.arguments();

		if (CollectionUtils.isEmpty(arguments)) {
			arguments = Collections.emptyMap();
		}

		return arguments;
	}

	@Override
	protected Mono<CallToolResult> convert(McpTransportContext context, CallToolRequest request, @Nullable Object result) {
		if (result == null) {
			return Mono.just(EMPTY_RESULT);
		}

		if (result instanceof CallToolResult toolResult) {
			return Mono.just(toolResult);
		}

		final ToolResponse response = convertContents(result);
		return Mono.just(response.toResult());
	}

	private ToolResponse convertContents(@Nullable Object result) {
		if (result == null) {
			return new ToolResponse();
		}

		if (result instanceof McpSchema.Content content) {
			return new ToolResponse(content);
		}

		if (result instanceof CharSequence content) {
			return new ToolResponse(content.toString());
		}

		if (result instanceof StructuredOutput<?> output) {
			final Map<String, ?> structuredContent;
			final String contents;

			try {
				structuredContent = mapper.convertValue(output, OBJECT_MAP_TYPE);
				contents = mapper.writeValueAsString(structuredContent);
			} catch (IOException ex) {
				throw new IllegalStateException("Could not write structured tool output", ex);
			}

			return new ToolResponse(contents, structuredContent);
		}

		final String contents;

		try {
			contents = mapper.writeValueAsString(result);
		} catch (IOException ex) {
			throw new IllegalStateException("Could not write tool output", ex);
		}

		return new ToolResponse(contents);
	}

	record ToolResponse(List<McpSchema.Content> contents, @Nullable Map<String, ?> structuredContent) {

		ToolResponse(McpSchema.Content... contents) {
			this(List.of(contents), null);
		}

		ToolResponse(String content) {
			this(McpSchema.TextContent.builder(content).build());
		}

		ToolResponse(String content, Map<String, ?> structuredContent) {
			this(List.of(McpSchema.TextContent.builder(content).build()), structuredContent);
		}

		CallToolResult toResult() {
			final CallToolResult.Builder builder = CallToolResult.builder(contents());

			if (structuredContent() != null) {
				builder.structuredContent(structuredContent());
			}

			return builder.build();
		}
	}

}
