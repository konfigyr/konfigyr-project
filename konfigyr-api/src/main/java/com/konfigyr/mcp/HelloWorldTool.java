package com.konfigyr.mcp;

import com.konfigyr.security.OAuthScope;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * A trivial MCP tool used to verify that the {@code /mcp} endpoint is reachable and correctly
 * secured behind the {@link OAuthScope#MCP} scope.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Component
class HelloWorldTool {

	static final McpSchema.Tool DEFINITION = McpSchema.Tool.builder("hello_world", Map.of(
			"type", "object",
			"properties", Map.of("name", Map.of("type", "string")),
			"additionalProperties", false
	)).description("Replies with a friendly greeting; used to verify the MCP server is reachable and secured.").build();

	McpSchema.CallToolResult call(McpSchema.CallToolRequest request) {
		final String name = (String) request.arguments().getOrDefault("name", "world");
		return McpSchema.CallToolResult.builder().addTextContent("Hello, " + name + "!").build();
	}

}
