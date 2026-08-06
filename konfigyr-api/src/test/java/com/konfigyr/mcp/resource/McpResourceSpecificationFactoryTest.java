package com.konfigyr.mcp.resource;

import com.konfigyr.artifactory.ArtifactoryJacksonModule;
import com.konfigyr.mcp.McpResourceFixtures;
import com.konfigyr.mcp.invoke.McpHandlerMethodFactory;
import com.konfigyr.mcp.registry.McpAnnotationRegistry;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.AsyncResourceTemplateSpecification;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.support.DefaultConversionService;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpResourceSpecificationFactoryTest {

	final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JsonMapper.builder()
			.addModule(new ArtifactoryJacksonModule())
			.build()
	);
	final McpHandlerMethodFactory handlerMethodFactory = new McpHandlerMethodFactory(jsonMapper, DefaultConversionService::getSharedInstance);
	final McpResourceSpecificationFactory factory = new McpResourceSpecificationFactory(jsonMapper, handlerMethodFactory);

	@Mock
	McpAnnotationRegistry registry;

	@Test
	@DisplayName("should build a resource template from the @McpResource annotation")
	void shouldBuildResourceTemplateFromAnnotation() {
		final ResourceTemplate template = createTemplate("manifest");

		assertThat(template.uriTemplate()).isEqualTo("konfigyr://services/{service}/manifest");
		assertThat(template.name()).isEqualTo("service_manifest");
		assertThat(template.title()).isEqualTo("Service Manifest");
		assertThat(template.description()).isEqualTo("Current manifest of a service");
	}

	@Test
	@DisplayName("should carry the mime type declared on the @McpResource annotation")
	void shouldCarryMimeTypeFromAnnotation() {
		assertThat(createTemplate("artifact").mimeType()).isEqualTo("application/json");
	}

	@Test
	@DisplayName("should build one specification per registration")
	void shouldBuildOneSpecificationPerRegistration() {
		when(registry.resources()).thenReturn(List.of(
				McpResourceFixtures.registration("manifest"),
				McpResourceFixtures.registration("artifact")
		));

		final List<AsyncResourceTemplateSpecification> specifications = factory.create(registry);

		assertThat(specifications)
				.extracting(specification -> specification.resourceTemplate().name())
				.containsExactlyInAnyOrder("service_manifest", "artifact_metadata");
	}

	private ResourceTemplate createTemplate(String methodName) {
		when(registry.resources()).thenReturn(List.of(McpResourceFixtures.registration(methodName)));

		final List<AsyncResourceTemplateSpecification> specifications = factory.create(registry);
		assertThat(specifications).hasSize(1);

		return specifications.getFirst().resourceTemplate();
	}

}
