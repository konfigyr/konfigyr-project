package com.konfigyr.mcp.resource;

import com.konfigyr.mcp.annotation.McpResource;
import com.konfigyr.mcp.invoke.McpHandlerMethodFactory;
import com.konfigyr.mcp.invoke.McpInvoker;
import com.konfigyr.mcp.registry.McpAnnotationRegistration;
import com.konfigyr.mcp.registry.McpAnnotationRegistry;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.AsyncResourceTemplateSpecification;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.util.DefaultMcpUriTemplateManagerFactory;
import io.modelcontextprotocol.util.McpUriTemplateManagerFactory;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
@RequiredArgsConstructor
public final class McpResourceSpecificationFactory {

	private final McpJsonMapper jsonMapper;
	private final McpHandlerMethodFactory handlerMethodFactory;
	private final McpUriTemplateManagerFactory templateManagerFactory = new DefaultMcpUriTemplateManagerFactory();

	public List<AsyncResourceTemplateSpecification> create(McpAnnotationRegistry registry) {
		return registry.resources().stream()
				.map(this::createSpecification)
				.toList();
	}

	private AsyncResourceTemplateSpecification createSpecification(McpAnnotationRegistration<McpResource> registration) {
		final ResourceTemplate resourceTemplate = createResourceTemplate(registration.annotation());
		final McpInvoker<ReadResourceRequest, ReadResourceResult> invoker = createInvoker(resourceTemplate, registration);
		return new AsyncResourceTemplateSpecification(resourceTemplate, invoker::invoke);
	}

	private McpInvoker<ReadResourceRequest, ReadResourceResult> createInvoker(
			ResourceTemplate resourceTemplate,
			McpAnnotationRegistration<McpResource> registration
	) {
		return new McpResourceInvoker(
				handlerMethodFactory.create(registration.bean(), registration.method()),
				jsonMapper, templateManagerFactory.create(resourceTemplate.uriTemplate()),
				resourceTemplate.mimeType()
		);
	}

	private static ResourceTemplate createResourceTemplate(McpResource resource) {
		return ResourceTemplate.builder(resource.uri(), resource.name())
				.title(resource.title())
				.description(resource.description())
				.mimeType(resource.mimeType())
				.build();
	}

}
