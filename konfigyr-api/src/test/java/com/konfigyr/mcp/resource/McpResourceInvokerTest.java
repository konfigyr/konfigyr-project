package com.konfigyr.mcp.resource;

import com.konfigyr.artifactory.ArtifactoryJacksonModule;
import com.konfigyr.io.ByteArray;
import com.konfigyr.mcp.McpResourceFixtures;
import com.konfigyr.mcp.invoke.McpHandlerMethod;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.BlobResourceContents;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.util.DefaultMcpUriTemplateManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpResourceInvokerTest {

	final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JsonMapper.builder()
			.addModule(new ArtifactoryJacksonModule())
			.build()
	);

	final ReadResourceRequest request = ReadResourceRequest.builder("konfigyr://services/payment-service/manifest").build();

	@Mock
	McpHandlerMethod method;

	McpResourceInvoker invoker;

	@BeforeEach
	void setup() {
		final var uriTemplateManager = new DefaultMcpUriTemplateManagerFactory()
				.create("konfigyr://services/{service}/manifest");

		invoker = new McpResourceInvoker(method, jsonMapper, uriTemplateManager, MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	@DisplayName("should extract the URI template variables and pass them as arguments to the handler method")
	void shouldExtractUriTemplateVariablesAsArguments() {
		when(method.invoke(Map.of("service", "payment-service"), McpTransportContext.EMPTY, request)).thenReturn("hello");

		invoker.invoke(McpTransportContext.EMPTY, request).block();

		verify(method).invoke(Map.of("service", "payment-service"), McpTransportContext.EMPTY, request);
	}

	@Test
	@DisplayName("should extract every variable from a multi-variable URI template")
	void shouldExtractMultipleUriTemplateVariables() {
		final var uriTemplateManager = new DefaultMcpUriTemplateManagerFactory()
				.create("konfigyr://artifacts/{groupId}/{artifactId}");

		final McpResourceInvoker invoker = new McpResourceInvoker(method, jsonMapper, uriTemplateManager, MediaType.APPLICATION_JSON_VALUE);
		final ReadResourceRequest artifactRequest = ReadResourceRequest.builder("konfigyr://artifacts/com.konfigyr/konfigyr-api").build();

		when(method.invoke(anyMap(), any(), any())).thenReturn("hello");

		invoker.invoke(McpTransportContext.EMPTY, artifactRequest).block();

		verify(method).invoke(
				Map.of("groupId", "com.konfigyr", "artifactId", "konfigyr-api"),
				McpTransportContext.EMPTY, artifactRequest
		);
	}

	@Test
	@DisplayName("should return an empty result when the handler method returns nothing")
	void shouldReturnEmptyResultWhenHandlerReturnsNull() {
		when(method.invoke(anyMap(), any(), any())).thenReturn(null);

		assertThat(invoke().contents()).isEmpty();
	}

	@Test
	@DisplayName("should return the handler's ReadResourceResult as-is")
	void shouldReturnReadResourceResultDirectly() {
		final ReadResourceResult handled = ReadResourceResult
				.builder(List.of(TextResourceContents.builder(request.uri(), "hi").build()))
				.build();
		when(method.invoke(anyMap(), any(), any())).thenReturn(handled);

		assertThat(invoke()).isSameAs(handled);
	}

	@Test
	@DisplayName("should wrap ResourceContents returned by the handler directly")
	void shouldWrapResourceContentsDirectly() {
		final TextResourceContents contents = TextResourceContents.builder(request.uri(), "hi").build();
		when(method.invoke(anyMap(), any(), any())).thenReturn(contents);

		assertThat(invoke().contents()).containsExactly(contents);
	}

	@Test
	@DisplayName("should flatten a collection returned by the handler into individual contents")
	void shouldFlattenCollectionIntoContents() {
		when(method.invoke(anyMap(), any(), any())).thenReturn(List.of("first", "second"));

		assertThat(invoke().contents()).containsExactly(
				TextResourceContents.builder(request.uri(), "first").mimeType("application/json").build(),
				TextResourceContents.builder(request.uri(), "second").mimeType("application/json").build()
		);
	}

	@Test
	@DisplayName("should wrap a String returned by the handler as text contents")
	void shouldWrapStringAsTextContents() {
		when(method.invoke(anyMap(), any(), any())).thenReturn("hello there");

		assertThat(invoke().contents()).containsExactly(
				TextResourceContents.builder(request.uri(), "hello there").mimeType("application/json").build()
		);
	}

	@Test
	@DisplayName("should wrap a ByteArray's contents returned by the handler as base64-encoded blob contents")
	void shouldWrapByteArrayAsBlobContents() {
		final var bytes = ByteArray.fromString("hello there");
		when(method.invoke(anyMap(), any(), any())).thenReturn(bytes);

		assertThat(invoke().contents()).containsExactly(
				BlobResourceContents.builder(request.uri(), bytes.encodeBase64())
						.mimeType("application/json")
						.build()
		);
	}

	@Test
	@DisplayName("should wrap an InputStreamSource's contents returned by the handler as base64-encoded blob contents")
	void shouldWrapInputStreamSourceAsBlobContents() {
		final byte[] bytes = "hello there".getBytes(StandardCharsets.UTF_8);
		when(method.invoke(anyMap(), any(), any())).thenReturn(new ByteArrayResource(bytes));

		assertThat(invoke().contents()).containsExactly(
				BlobResourceContents.builder(request.uri(), Base64.getEncoder().encodeToString(bytes))
						.mimeType("application/json")
						.build()
		);
	}

	@Test
	@DisplayName("should serialize an arbitrary object returned by the handler as JSON text contents")
	void shouldSerializeArbitraryObjectAsJsonTextContents() throws Exception {
		final McpResourceFixtures.Manifest manifest = McpResourceFixtures.BEAN.manifest("payment-service");
		when(method.invoke(anyMap(), any(), any())).thenReturn(manifest);

		final String expected = jsonMapper.writeValueAsString(manifest);

		assertThat(invoke().contents()).containsExactly(
				TextResourceContents.builder(request.uri(), expected).mimeType("application/json").build()
		);
	}

	@Test
	@DisplayName("should unwrap a reactive return value before converting it")
	void shouldUnwrapReactiveReturnValue() {
		when(method.invoke(anyMap(), any(), any())).thenReturn(Mono.just("hello there"));

		assertThat(invoke().contents()).containsExactly(
				TextResourceContents.builder(request.uri(), "hello there").mimeType("application/json").build()
		);
	}

	private ReadResourceResult invoke() {
		return invoker.invoke(McpTransportContext.EMPTY, request).block();
	}

}
