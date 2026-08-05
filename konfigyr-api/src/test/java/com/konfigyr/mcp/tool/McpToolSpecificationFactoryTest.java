package com.konfigyr.mcp.tool;

import com.konfigyr.artifactory.ArtifactoryJacksonModule;
import com.konfigyr.mcp.McpToolFixtures;
import com.konfigyr.mcp.invoke.McpHandlerMethodFactory;
import com.konfigyr.mcp.registry.McpAnnotationRegistry;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.support.DefaultConversionService;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpToolSpecificationFactoryTest {

	final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JsonMapper.builder()
			.addModule(new ArtifactoryJacksonModule())
			.build()
	);
	final McpHandlerMethodFactory handlerMethodFactory = new McpHandlerMethodFactory(jsonMapper, DefaultConversionService::getSharedInstance);
	final McpToolSpecificationFactory factory = new McpToolSpecificationFactory(jsonMapper, handlerMethodFactory);

	@Mock
	McpAnnotationRegistry registry;

	@Test
	@DisplayName("should build a tool template from the @McpTool annotation")
	void shouldBuildToolTemplateFromAnnotation() {
		final Tool tool = createTool("greet");

		assertThat(tool.name()).isEqualTo("greet");
		assertThat(tool.title()).isEqualTo("Greet");
		assertThat(tool.description()).isEqualTo("Greets someone");
	}

	@Test
	@DisplayName("should map every @McpTool hint flag onto the tool's annotations")
	void shouldMapToolHintsOntoAnnotations() {
		final Tool greet = createTool("greet");

		assertThat(greet.annotations().readOnlyHint()).isTrue();
		assertThat(greet.annotations().destructiveHint()).isFalse();
		assertThat(greet.annotations().idempotentHint()).isTrue();
		assertThat(greet.annotations().openWorldHint()).isTrue();

		final Tool destroy = createTool("destroy");

		assertThat(destroy.annotations().readOnlyHint()).isFalse();
		assertThat(destroy.annotations().destructiveHint()).isTrue();
		assertThat(destroy.annotations().idempotentHint()).isFalse();
		assertThat(destroy.annotations().openWorldHint()).isFalse();
	}

	@Test
	@DisplayName("should mark a @McpToolParam parameter as required by default")
	void shouldMarkToolParamAsRequiredByDefault() {
		final Map<String, Object> inputSchema = createTool("greet").inputSchema();

		assertThat(inputSchema).containsEntry("required", List.of("name"));
		assertThat(properties(inputSchema)).containsOnlyKeys("name");
	}

	@Test
	@DisplayName("should not require a @McpToolParam(required = false) parameter")
	void shouldNotRequireOptionalToolParam() {
		final Map<String, Object> inputSchema = createTool("destroy").inputSchema();

		assertThat(inputSchema).doesNotContainKey("required");
		assertThat(properties(inputSchema)).containsOnlyKeys("timeout");
	}

	@Test
	@DisplayName("should not generate an output schema for a tool returning a plain domain type")
	void shouldSkipOutputSchemaForPlainReturnType() {
		assertThat(createTool("greet").outputSchema()).isNull();
	}

	@Test
	@DisplayName("should not generate an output schema for a tool returning void")
	void shouldSkipOutputSchemaForVoidReturnType() {
		assertThat(createTool("destroy").outputSchema()).isNull();
	}

	@Test
	@DisplayName("should generate an output schema transparently unwrapping a StructuredEntityOutput return type")
	void shouldGenerateOutputSchemaForStructuredEntityOutput() {
		final Map<String, Object> outputSchema = createTool("find").outputSchema();

		assertThat(outputSchema).isNotNull();
		assertThat(properties(outputSchema)).containsOnlyKeys("message");

		// Greeting.message() is a String, and only primitive components are marked as required
		assertThat(outputSchema).doesNotContainKey("required");
	}

	@Test
	@DisplayName("should generate an output schema wrapping a StructuredCollectionOutput return type in a contents array")
	void shouldGenerateOutputSchemaForStructuredCollectionOutput() {
		final Map<String, Object> outputSchema = createTool("list").outputSchema();

		assertThat(outputSchema).isNotNull();
		assertThat(outputSchema).containsEntry("required", List.of("contents"));
		assertThat(properties(outputSchema)).containsOnlyKeys("contents");

		final Map<String, Object> contentsSchema = property(outputSchema, "contents");
		assertThat(contentsSchema).containsEntry("type", "array");
		assertThat(properties(items(contentsSchema))).containsOnlyKeys("message");
	}

	private Tool createTool(String methodName) {
		when(registry.tools()).thenReturn(List.of(McpToolFixtures.registration(methodName)));

		final List<AsyncToolSpecification> specifications = factory.create(registry);
		assertThat(specifications).hasSize(1);

		return specifications.getFirst().tool();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> properties(Map<String, Object> schema) {
		return (Map<String, Object>) schema.get("properties");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> property(Map<String, Object> schema, String name) {
		return (Map<String, Object>) properties(schema).get(name);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> items(Map<String, Object> arraySchema) {
		return (Map<String, Object>) arraySchema.get("items");
	}

}
