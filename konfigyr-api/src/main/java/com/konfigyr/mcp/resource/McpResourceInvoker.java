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
