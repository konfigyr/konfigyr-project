package com.konfigyr.mcp.registry;

import com.konfigyr.mcp.McpToolFixtures;
import com.konfigyr.mcp.annotation.McpComponent;
import com.konfigyr.mcp.annotation.McpResource;
import com.konfigyr.mcp.annotation.McpTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class McpAnnotationBeanPostProcessorTest {

	McpAnnotationRegistry registry;
	McpAnnotationBeanPostProcessor processor;

	@BeforeEach
	void setup() {
		registry = new McpAnnotationRegistry();
		processor = new McpAnnotationBeanPostProcessor(registry);
	}

	@Test
	@DisplayName("should register resource and tool methods for a bean annotated with @McpComponent")
	void shouldRegisterAnnotatedMethods() {
		final AnnotatedBean bean = new AnnotatedBean();

		assertThat(processor.postProcessAfterInitialization(bean, "annotatedBean"))
				.as("Post processor should return the original bean")
				.isSameAs(bean);

		assertThat(registry.resources())
				.hasSize(1)
				.extracting(McpAnnotationRegistration::name)
				.containsExactlyInAnyOrder("test_resource");

		assertThat(registry.tools())
				.hasSize(1)
				.extracting(McpAnnotationRegistration::name)
				.containsExactlyInAnyOrder("test_tool");
	}

	@Test
	@DisplayName("should not register any methods for a bean not annotated with @McpComponent")
	void shouldSkipBeanWithoutMcpComponent() {
		final McpToolFixtures.ToolBean bean = McpToolFixtures.BEAN;

		assertThat(processor.postProcessAfterInitialization(bean, "toolBean"))
				.as("Post processor should return the original bean")
				.isSameAs(bean);

		assertThat(registry.resources()).isEmpty();
		assertThat(registry.tools()).isEmpty();
	}

	@McpComponent
	static class AnnotatedBean {

		@McpResource(uri = "konfigyr://test/resource", name = "test_resource")
		String resource() {
			return "resource";
		}

		@McpTool(name = "test_tool")
		String tool() {
			return "tool";
		}

		void notAnnotated() {
			// should be ignored as no MCP annotation is present
		}

	}

}
