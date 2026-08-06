package com.konfigyr.mcp.resource;

import com.konfigyr.io.ByteArray;
import com.konfigyr.mcp.invoke.AbstractMcpHandlerMethodInvoker;
import com.konfigyr.mcp.invoke.McpHandlerMethod;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.util.McpUriTemplateManager;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link AbstractMcpHandlerMethodInvoker} for {@code resources/read} requests.
 * <p>
 * Extracts URI template variables as the handler method's arguments, then converts its return
 * value into one or more {@link McpSchema.ResourceContents}: a {@link String} becomes text
 * content, a {@link ByteArray} or {@link InputStreamSource} is read and base64-encoded into a
 * blob, a {@link Collection} is flattened, and anything else is serialized to JSON. The
 * declared {@code mimeType} is applied to every content produced this way, except the JSON
 * fallback, which is always {@code application/json}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
final class McpResourceInvoker extends AbstractMcpHandlerMethodInvoker<ReadResourceRequest, ReadResourceResult> {

	static McpSchema.ReadResourceResult EMPTY_RESULT = McpSchema.ReadResourceResult
			.builder(List.of())
			.build();

	private final McpJsonMapper mapper;
	private final McpUriTemplateManager uriTemplateManager;
	private final String mimeType;

	McpResourceInvoker(
			McpHandlerMethod method, McpJsonMapper jsonMapper,
			McpUriTemplateManager uriTemplateManager, String mimeType
	) {
		super(method);
		this.mapper = jsonMapper;
		this.uriTemplateManager = uriTemplateManager;
		this.mimeType = mimeType;
	}

	@Override
	protected Map<String, ?> constructArguments(McpTransportContext context, ReadResourceRequest request) {
		return uriTemplateManager.extractVariableValues(request.uri());
	}

	@Override
	protected Mono<ReadResourceResult> convert(McpTransportContext context, ReadResourceRequest request, @Nullable Object result) {
		if (result == null) {
			return Mono.just(EMPTY_RESULT);
		}

		if (result instanceof McpSchema.ReadResourceResult resourceResult) {
			return Mono.just(resourceResult);
		}

		return Mono.just(
				McpSchema.ReadResourceResult
						.builder(convertContents(request.uri(), result))
						.build()
		);
	}

	private List<McpSchema.ResourceContents> convertContents(String requestUri, @Nullable Object result) {
		if (result == null) {
			return Collections.emptyList();
		}

		if (result instanceof McpSchema.ResourceContents resourceContents) {
			return List.of(resourceContents);
		}

		if (result instanceof Collection<?> collection) {
			return collection.stream()
					.map(it -> convertContents(requestUri, it))
					.flatMap(Collection::stream)
					.toList();
		}

		if (result instanceof String content) {
			return List.of(
					McpSchema.TextResourceContents.builder(requestUri, content)
							.mimeType(mimeType)
							.build()
			);
		}

		if (result instanceof ByteArray bytes) {
			return List.of(
					McpSchema.BlobResourceContents.builder(requestUri, bytes.encodeBase64())
							.mimeType(mimeType)
							.build()
			);
		}

		if (result instanceof InputStreamSource resource) {
			final byte[] bytes;

			try {
				bytes = IOUtils.toByteArray(resource.getInputStream());
			} catch (IOException ex) {
				throw new IllegalStateException("Could not read resource contents", ex);
			}

			return List.of(
					McpSchema.BlobResourceContents.builder(requestUri, Base64.getEncoder().encodeToString(bytes))
							.mimeType(mimeType)
							.build()
			);
		}

		final String contents;

		try {
			contents = mapper.writeValueAsString(result);
		} catch (IOException ex) {
			throw new IllegalStateException("Could not write resource response", ex);
		}

		return List.of(
				McpSchema.TextResourceContents.builder(requestUri, contents)
						.mimeType(MediaType.APPLICATION_JSON_VALUE)
						.build()
		);
	}

}
