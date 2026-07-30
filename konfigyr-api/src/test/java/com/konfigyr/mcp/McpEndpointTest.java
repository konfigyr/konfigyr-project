package com.konfigyr.mcp;

import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import io.modelcontextprotocol.spec.McpSchema;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class McpEndpointTest extends AbstractControllerTest {

	private static final String CALL_HELLO_WORLD = """
			{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"hello_world","arguments":{"name":"Vladimir"}}}""";

	@Test
	@DisplayName("should reject MCP request without authentication")
	void shouldRejectRequestWithoutAuthentication() {
		mvc.post().uri("/mcp")
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CALL_HELLO_WORLD)
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(unauthorized());
	}

	@Test
	@DisplayName("should reject MCP request when MCP scope is missing")
	void shouldRejectToolCallWithoutMcpScope() {
		mvc.post().uri("/mcp")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CALL_HELLO_WORLD)
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.MCP));
	}

	@Test
	@DisplayName("should reject MCP request when 'application/json' content type is missing from Accept header")
	void shouldRejectToolCallWithoutJsonHeader() {
		mvc.post().uri("/mcp")
				.with(authentication(TestPrincipals.john(), OAuthScope.MCP))
				.accept(MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CALL_HELLO_WORLD)
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.hasPathSatisfying("$.id", path -> path.assertThat()
						.isNull())
				.hasPathSatisfying("$.jsonrpc", path -> path.assertThat()
						.isEqualTo(McpSchema.JSONRPC_VERSION))
				.hasPathSatisfying("$.error.code", path -> path.assertThat()
						.isEqualTo(McpSchema.ErrorCodes.INVALID_REQUEST))
				.hasPathSatisfying("$.error.message", path -> path.assertThat()
						.isEqualTo("Both 'application/json' and 'text/event-stream' required in the 'Accept' header"));
	}

	@Test
	@DisplayName("should reject MCP request when 'text/event-stream' content type is missing from Accept header")
	void shouldRejectToolCallWithoutEventStreamHeader() {
		mvc.post().uri("/mcp")
				.with(authentication(TestPrincipals.john(), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CALL_HELLO_WORLD)
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.hasPathSatisfying("$.id", path -> path.assertThat()
						.isNull())
				.hasPathSatisfying("$.jsonrpc", path -> path.assertThat()
						.isEqualTo(McpSchema.JSONRPC_VERSION))
				.hasPathSatisfying("$.error.code", path -> path.assertThat()
						.isEqualTo(McpSchema.ErrorCodes.INVALID_REQUEST))
				.hasPathSatisfying("$.error.message", path -> path.assertThat()
						.isEqualTo("Both 'application/json' and 'text/event-stream' required in the 'Accept' header"));
	}

	@Test
	@DisplayName("should list registered tools when MCP scope is granted")
	void shouldListToolsWithMcpScope() {
		mvc.post().uri("/mcp")
				.with(authentication(TestPrincipals.john(), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result")
				.convertTo(McpSchema.ListToolsResult.class)
				.extracting(McpSchema.ListToolsResult::tools, InstanceOfAssertFactories.iterable(McpSchema.Tool.class))
				.containsExactlyInAnyOrder(HelloWorldTool.DEFINITION);
	}

	@Test
	@DisplayName("should invoke hello_world tool when MCP scope is granted")
	void shouldInvokeHelloWorldToolWithMcpScope() {
		mvc.post().uri("/mcp")
				.with(authentication(TestPrincipals.john(), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CALL_HELLO_WORLD)
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result")
				.convertTo(McpSchema.CallToolResult.class)
				.returns(false, McpSchema.CallToolResult::isError)
				.extracting(McpSchema.CallToolResult::content, InstanceOfAssertFactories.iterable(McpSchema.Content.class))
				.containsExactly(McpSchema.TextContent.builder("Hello, Vladimir!").build());
	}

}
