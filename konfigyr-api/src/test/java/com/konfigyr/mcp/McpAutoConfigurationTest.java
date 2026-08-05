package com.konfigyr.mcp;

import com.konfigyr.mcp.registry.McpAnnotationBeanPostProcessor;
import com.konfigyr.mcp.registry.McpAnnotationRegistry;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class McpAutoConfigurationTest {

	@Mock
	McpStatelessServerTransport transport;

	@Mock
	McpTools helloWorldTool;

	ApplicationContextRunner runner;

	@BeforeEach
	void setup() {
		runner = new ApplicationContextRunner()
				.withBean(McpStatelessServerTransport.class, () -> transport)
				.withBean(McpTools.class, () -> helloWorldTool)
				.withBean(JsonMapper.class, JsonMapper::shared)
				.withConfiguration(AutoConfigurations.of(McpAutoConfiguration.class))
				.withPropertyValues(
						"konfigyr.mcp.version=1.0.0",
						"konfigyr.mcp.instructions=Use these tools responsibly."
				);
	}

	@Test
	@DisplayName("should register MCP server beans when enabled")
	void shouldRegisterMcpServerBeans() {
		runner.run(context -> assertThat(context)
				.hasNotFailed()
				.hasSingleBean(McpJsonMapper.class)
				.hasSingleBean(McpAnnotationRegistry.class)
				.hasSingleBean(McpAnnotationBeanPostProcessor.class)
				.hasSingleBean(McpStatelessAsyncServer.class)
		);
	}

	@Test
	@DisplayName("should register MCP related beans when 'konfigyr.mcp.enabled' is explicitly set")
	void shouldRegisterMcpServerBeansWhenEnabledPropertyIsMissing() {
		runner.withPropertyValues("konfigyr.mcp.enabled=true")
				.run(context -> assertThat(context)
						.hasNotFailed()
						.hasSingleBean(McpJsonMapper.class)
						.hasSingleBean(McpAnnotationRegistry.class)
						.hasSingleBean(McpAnnotationBeanPostProcessor.class)
						.hasSingleBean(McpStatelessAsyncServer.class)
				);
	}

	@Test
	@DisplayName("should not register any MCP server beans when disabled")
	void shouldNotRegisterBeansWhenDisabled() {
		runner.withPropertyValues("konfigyr.mcp.enabled=false")
				.run(context -> assertThat(context)
						.doesNotHaveBean(McpJsonMapper.class)
						.doesNotHaveBean(McpStatelessSyncServer.class)
				);
	}

	@Test
	@DisplayName("should fail to start when required MCP properties are missing")
	void shouldFailToStartWithoutRequiredProperties() {
		runner.withPropertyValues("konfigyr.mcp.name=")
				.run(context -> assertThat(context)
						.hasFailed()
						.getFailure()
						.hasRootCauseInstanceOf(BindValidationException.class)
						.hasMessageContaining("konfigyr.mcp")
				);
	}

}
