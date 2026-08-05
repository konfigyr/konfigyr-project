package com.konfigyr.mcp.tool;

import com.konfigyr.mcp.McpToolFixtures;
import com.konfigyr.mcp.invoke.McpHandlerMethod;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpToolInvokerTest {

	final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JsonMapper.shared());
	final CallToolRequest request = CallToolRequest.builder("greet")
			.arguments(Map.of("name", "John Doe"))
			.build();

	@Mock
	McpHandlerMethod method;

	McpToolInvoker invoker;

	@BeforeEach
	void setup() {
		invoker = new McpToolInvoker(method, jsonMapper);
	}

	@Test
	@DisplayName("should pass the request's arguments directly to the handler method")
	void shouldPassRequestArgumentsDirectlyToHandlerMethod() {
		when(method.invoke(request.arguments(), McpTransportContext.EMPTY, request))
				.thenReturn("hello");

		assertThatResult().isNotNull();

		verify(method).invoke(request.arguments(), McpTransportContext.EMPTY, request);
	}

	@Test
	@DisplayName("should return an empty result when the handler method returns nothing")
	void shouldReturnEmptyResultWhenHandlerReturnsNull() {
		when(method.invoke(anyMap(), any(), any())).thenReturn(null);

		assertThatContents().isEmpty();
	}

	@Test
	@DisplayName("should return the handler's CallToolResult as-is")
	void shouldReturnCallToolResultDirectly() {
		final CallToolResult original = mock(CallToolResult.class);
		when(method.invoke(anyMap(), any(), any())).thenReturn(original);

		assertThatResult().isSameAs(original);
	}

	@Test
	@DisplayName("should wrap a Content returned by the handler directly")
	void shouldWrapContentDirectly() {
		final TextContent content = TextContent.builder("hi").build();
		when(method.invoke(anyMap(), any(), any())).thenReturn(content);

		assertThatContents()
				.containsExactly(content);
	}

	@Test
	@DisplayName("should wrap a String returned by the handler as text content")
	void shouldWrapStringAsTextContent() {
		when(method.invoke(anyMap(), any(), any())).thenReturn("hello there");

		assertThatContents()
				.containsExactly(TextContent.builder("hello there").build());
	}

	@Test
	@DisplayName("should serialize an arbitrary object returned by the handler as JSON text content")
	void shouldSerializeArbitraryObjectAsJsonTextContent() throws Exception {
		final var greeting = new McpToolFixtures.Greeting("John Doe");
		when(method.invoke(anyMap(), any(), any())).thenReturn(greeting);

		final String expected = jsonMapper.writeValueAsString(greeting);

		assertThatContents()
				.containsExactly(TextContent.builder(expected).build());
	}

	@Test
	@DisplayName("should unwrap a reactive return value before converting it")
	void shouldUnwrapReactiveReturnValue() {
		when(method.invoke(anyMap(), any(), any())).thenReturn(Mono.just("hello there"));

		assertThatContents()
				.containsExactly(TextContent.builder("hello there").build());
	}

	private ObjectAssert<CallToolResult> assertThatResult() {
		return assertThat(invoker.invoke(McpTransportContext.EMPTY, request).block());
	}

	private ListAssert<Content> assertThatContents() {
		return assertThatResult().extracting(CallToolResult::content, InstanceOfAssertFactories.list(Content.class));
	}

}
