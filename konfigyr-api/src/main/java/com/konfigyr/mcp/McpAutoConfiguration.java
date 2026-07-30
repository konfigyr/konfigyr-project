package com.konfigyr.mcp;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Autoconfiguration that exposes the Konfigyr MCP server over the stateless {@code /mcp} endpoint
 * registered by {@link com.konfigyr.mcp.McpEndpoint}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@AutoConfiguration
class McpAutoConfiguration {

	@Bean
	McpJsonMapper mcpJsonMapper(JsonMapper mapper) {
		return new JacksonMcpJsonMapper(
				mapper.rebuild()
						.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
						.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
						.build()
		);
	}

	@Bean(destroyMethod = "close")
	McpStatelessSyncServer mcpServer(
			BuildProperties properties,
			McpJsonMapper mcpJsonMapper,
			McpStatelessServerTransport transport,
			HelloWorldTool helloWorldTool
	) {
		return McpServer.sync(transport)
				.serverInfo("konfigyr-api", properties.getVersion())
				.jsonMapper(mcpJsonMapper)
				.capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
				.toolCall(HelloWorldTool.DEFINITION, (context, request) -> helloWorldTool.call(request))
				.build();
	}

}
