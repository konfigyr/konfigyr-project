package com.konfigyr.mcp.tool;

import com.konfigyr.artifactory.JsonSchema;
import com.konfigyr.artifactory.ObjectSchema;
import com.konfigyr.mcp.annotation.McpTool;
import com.konfigyr.mcp.annotation.McpToolParam;
import com.konfigyr.mcp.invoke.McpHandlerMethod;
import com.konfigyr.mcp.invoke.McpHandlerMethodFactory;
import com.konfigyr.mcp.registry.McpAnnotationRegistration;
import com.konfigyr.mcp.registry.McpAnnotationRegistry;
import com.konfigyr.mcp.schema.JsonSchemaGenerator;
import com.konfigyr.mcp.schema.JsonSchemaGeneratorHints;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;

import java.util.List;
import java.util.Map;

@NullMarked
@RequiredArgsConstructor
public final class McpToolSpecificationFactory {

	static final TypeRef<Map<String, Object>> JSON_SCHEMA_TYPE = new TypeRef<>() {};

	private final McpJsonMapper jsonMapper;
	private final McpHandlerMethodFactory handlerMethodFactory;
	private final JsonSchemaGenerator jsonSchemaGenerator = JsonSchemaGenerator.withProviders(
			new StructuredOutputJsonSchemaDefinitionProvider()
	);

	public List<AsyncToolSpecification> create(McpAnnotationRegistry registry) {
		return registry.tools().stream()
				.map(this::createSpecification)
				.toList();
	}

	private AsyncToolSpecification createSpecification(McpAnnotationRegistration<McpTool> registration) {
		final McpHandlerMethod method = handlerMethodFactory.create(registration.bean(), registration.method());
		final Tool toolTemplate = createToolTemplate(registration.annotation(), createInputSchema(method),
				createOutputSchema(registration.annotation(), method));

		return new AsyncToolSpecification(toolTemplate, new McpToolInvoker(method, jsonMapper)::invoke);
	}

	private Map<String, Object> createInputSchema(McpHandlerMethod method) {
		final ObjectSchema.Builder builder = ObjectSchema.builder();

		method.parameters().forEach(parameter -> {
			final MergedAnnotation<McpToolParam> annotation = parameter.annotation(McpToolParam.class);

			if (annotation.isPresent()) {
				final JsonSchemaGeneratorHints hints = new JsonSchemaGeneratorHints(
						annotation.getString("description")
				);

				final JsonSchema schema = jsonSchemaGenerator.generate(parameter.type(), hints);

				if (annotation.getBoolean("required")) {
					builder.required(parameter.name());
				}

				builder.property(parameter.name(), schema);
			}
		});

		return jsonMapper.convertValue(builder.build(), JSON_SCHEMA_TYPE);
	}

	@Nullable
	private Map<String, Object> createOutputSchema(McpTool tool, McpHandlerMethod method) {
		final ResolvableType returnType = method.returnType();

		if (tool.generateOutputSchema() && StructuredOutput.class.isAssignableFrom(returnType.toClass())) {
			final JsonSchema schema = jsonSchemaGenerator.generate(returnType);
			return jsonMapper.convertValue(schema, JSON_SCHEMA_TYPE);
		}

		return null;
	}

	private static Tool createToolTemplate(McpTool tool, Map<String, Object> inputSchema, @Nullable Map<String, Object> outputSchema) {
		return Tool.builder(tool.name(), inputSchema)
				.title(tool.title())
				.description(tool.description())
				.outputSchema(outputSchema)
				.annotations(ToolAnnotations.builder()
						.title(tool.title())
						.destructiveHint(tool.destructiveHint())
						.openWorldHint(tool.openWorldHint())
						.idempotentHint(tool.idempotentHint())
						.readOnlyHint(tool.readOnlyHint())
						.build()
				)
				.build();
	}

}
