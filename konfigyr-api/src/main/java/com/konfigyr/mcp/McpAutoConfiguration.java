package com.konfigyr.mcp;

import com.konfigyr.artifactory.ArtifactoryJacksonModule;
import com.konfigyr.crypto.CryptographyJacksonModule;
import com.konfigyr.mcp.annotation.ConditionalOnMcpServer;
import com.konfigyr.mcp.invoke.McpHandlerMethodFactory;
import com.konfigyr.mcp.registry.McpAnnotationBeanPostProcessor;
import com.konfigyr.mcp.registry.McpAnnotationRegistry;
import com.konfigyr.mcp.resource.McpResourceSpecificationFactory;
import com.konfigyr.mcp.tool.McpToolSpecificationFactory;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ResourceLoader;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Autoconfiguration that exposes the Konfigyr MCP server over the stateless {@code /mcp} endpoint
 * registered by {@link com.konfigyr.mcp.McpEndpoint}.
 * <p>
 * Registers {@code META-INF/build-info.properties} as a property source so that {@code build.*}
 * placeholders (e.g. {@code ${build.version}}) can be referenced from {@code konfigyr.mcp.*}
 * properties - {@link org.springframework.boot.info.BuildProperties} alone doesn't make those
 * values available for placeholder resolution.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnMcpServer
@EnableConfigurationProperties(McpProperties.class)
class McpAutoConfiguration {

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	static McpAnnotationRegistry mcpAnnotationRegistry() {
		return new McpAnnotationRegistry();
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	static McpAnnotationBeanPostProcessor mcpAnnotationBeanPostProcessor(McpAnnotationRegistry registry) {
		return new McpAnnotationBeanPostProcessor(registry);
	}

	@Bean
	McpJsonMapper mcpJsonMapper(ResourceLoader resourceLoader) {
		return new JacksonMcpJsonMapper(
				JsonMapper.builder()
						.addModule(new McpJacksonModule())
						.addModule(new ArtifactoryJacksonModule())
						.addModule(new CryptographyJacksonModule())
						.findAndAddModules(resourceLoader.getClassLoader())
						.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
						.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
						.build()
		);
	}

	@Bean
	McpHandlerMethodFactory mcpHandlerMethodFactory(McpJsonMapper jsonMapper, ObjectProvider<ConversionService> converter) {
		return new McpHandlerMethodFactory(jsonMapper, converter::getObject);
	}

	@Bean
	McpResourceSpecificationFactory mcpResourceSpecificationFactory(McpJsonMapper jsonMapper, McpHandlerMethodFactory handlerMethodFactory) {
		return new McpResourceSpecificationFactory(jsonMapper, handlerMethodFactory);
	}

	@Bean
	McpToolSpecificationFactory mcpToolSpecificationFactory(McpJsonMapper jsonMapper, McpHandlerMethodFactory handlerMethodFactory) {
		return new McpToolSpecificationFactory(jsonMapper, handlerMethodFactory);
	}

	@Bean(destroyMethod = "close")
	McpStatelessAsyncServer mcpServer(
			McpJsonMapper mapper,
			McpProperties properties,
			McpAnnotationRegistry registry,
			McpStatelessServerTransport transport,
			McpResourceSpecificationFactory resourceSpecificationFactory,
			McpToolSpecificationFactory toolSpecificationFactory
	) {
		return McpServer.async(transport)
				.jsonMapper(mapper)
				.serverInfo(serverInfo(properties))
				.instructions(properties.getInstructions())
				.capabilities(capabilities(properties.getCapabilities()))
				.tools(toolSpecificationFactory.create(registry))
				.resourceTemplates(resourceSpecificationFactory.create(registry))
				.build();
	}

	private static McpSchema.Implementation serverInfo(McpProperties properties) {
		return McpSchema.Implementation.builder(properties.getName(), properties.getVersion())
				.description(properties.getDescription())
				.websiteUrl(properties.getWebsiteUrl())
				.build();
	}

	private static McpSchema.ServerCapabilities capabilities(McpProperties.Capabilities capabilities) {
		final McpSchema.ServerCapabilities.Builder builder = McpSchema.ServerCapabilities.builder();

		if (capabilities.isTool()) {
			builder.tools(false);
		}

		if (capabilities.isResource()) {
			builder.resources(false, false);
		}

		if (capabilities.isPrompt()) {
			builder.prompts(false);
		}

		if (capabilities.isCompletion()) {
			builder.completions();
		}

		return builder.build();
	}

}
