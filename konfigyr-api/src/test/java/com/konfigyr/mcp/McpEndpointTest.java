package com.konfigyr.mcp;

import com.konfigyr.entity.EntityId;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.TestPrincipals;
import io.modelcontextprotocol.spec.McpSchema;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class McpEndpointTest extends AbstractMcpTest {

	private static final McpSchema.CallToolRequest DUMMY_TOOL_CALL = McpSchema.CallToolRequest
			.builder("dummy-tool")
			.build();

	@Autowired
	McpProperties properties;

	@Test
	@DisplayName("should reject MCP request without authentication")
	void shouldRejectRequestWithoutAuthentication() {
		mvc.post().uri("/mcp")
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, DUMMY_TOOL_CALL))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(unauthorized());
	}

	@Test
	@DisplayName("should reject MCP request without a namespace claim in the OAuth2 client authentication")
	void shouldRejectRequestWithoutNamespaceClaim() {
		mvc.post().uri("/mcp")
				.with(authentication(TestPrincipals.john(), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, DUMMY_TOOL_CALL))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should reject MCP request without a valid namespace claim in the OAuth2 client authentication")
	void shouldRejectRequestUnknownNamespaceClaim() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(99999), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, DUMMY_TOOL_CALL))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should reject MCP request when MCP scope is missing")
	void shouldRejectToolCallWithoutMcpScope() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.READ_NAMESPACES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, DUMMY_TOOL_CALL))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.MCP));
	}

	@Test
	@DisplayName("should reject MCP request when 'application/json' content type is missing from Accept header")
	void shouldRejectToolCallWithoutJsonHeader() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(1), OAuthScope.MCP))
				.accept(MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, DUMMY_TOOL_CALL))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.BAD_REQUEST, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INVALID_REQUEST)
						.hasMessage("Both 'application/json' and 'text/event-stream' required in the 'Accept' header")
				));
	}

	@Test
	@DisplayName("should reject MCP request when 'text/event-stream' content type is missing from Accept header")
	void shouldRejectToolCallWithoutEventStreamHeader() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, DUMMY_TOOL_CALL))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.BAD_REQUEST, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INVALID_REQUEST)
						.hasMessage("Both 'application/json' and 'text/event-stream' required in the 'Accept' header")
				));
	}

	@Test
	@DisplayName("should reject MCP request with a malformed JSON body")
	void shouldRejectMalformedJsonBody() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not valid json")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.BAD_REQUEST, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.PARSE_ERROR)
						.hasMessage("Could not parse incoming MCP JSON-RPC message")
				));
	}

	@Test
	@DisplayName("should reject MCP request whose body matches no known JSON-RPC message shape")
	void shouldRejectUnrecognizedJsonRpcShape() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"foo\":\"bar\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.BAD_REQUEST, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.PARSE_ERROR)
						.hasMessageContaining("Could not parse")
				));
	}

	@Test
	@DisplayName("should reject MCP request whose body is a JSON-RPC response instead of a request or notification")
	void shouldRejectJsonRpcResponseMessage() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.BAD_REQUEST, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INVALID_REQUEST)
						.hasMessage("The server accepts either requests or notifications")
				));
	}

	@Test
	@DisplayName("should reject MCP request due to unknown request handler for method")
	void shouldRejectInvalidJsonRpcRequest() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest("unknown_method", null))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Unexpected error occurred while handling JSON RPC request")
				));
	}

	@Test
	@DisplayName("should return HTTP 200 with a JSON-RPC error body when the MCP SDK reports a protocol-level error")
	void shouldReturnOkForProtocolLevelJsonRpcError() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, DUMMY_TOOL_CALL))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.OK, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INVALID_PARAMS)
						.hasMessage("Unknown tool: invalid_tool_name")
						.hasData("Tool not found: dummy-tool")
				));
	}

	@Test
	@DisplayName("should accept MCP notification when MCP scope is granted")
	void shouldAcceptNotificationWithMcpScope() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.ACCEPTED);
	}

	@Test
	@DisplayName("should list registered tools when MCP scope is granted")
	void shouldListToolsWithMcpScope() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_LIST, null))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result")
				.convertTo(McpSchema.ListToolsResult.class)
				.extracting(McpSchema.ListToolsResult::tools, InstanceOfAssertFactories.iterable(McpSchema.Tool.class))
				.extracting(McpSchema.Tool::name, McpSchema.Tool::title)
				.containsExactlyInAnyOrder(
						tuple("list_services", "List services"),
						tuple("list_profiles", "List service profiles"),
						tuple("list_change_requests", "List change requests"),
						tuple("get_change_request", "Retrieve change request")
				);
	}

	@Test
	@DisplayName("should list registered resources when MCP scope is granted")
	void shouldListTemplatedResourcesWithMcpScope() {
		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, null))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result")
				.convertTo(McpSchema.ListResourceTemplatesResult.class)
				.extracting(McpSchema.ListResourceTemplatesResult::resourceTemplates, InstanceOfAssertFactories.iterable(McpSchema.ResourceTemplate.class))
				.containsExactlyInAnyOrder(
						McpSchema.ResourceTemplate
								.builder("konfigyr://artifacts/{groupId}/{artifactId}/{version}", "artifact_metadata")
								.title("Artifact metadata")
								.description("Full property list for one exact artifact version, identified by the 'groupId', 'artifactId', " +
										"and 'version' URI variables (standard Maven-style coordinates). A not-found result (unknown/unpublished " +
										"coordinates, or a private artifact outside your namespace) is a normal empty read, not an error - " +
										"treat it as 'no properties known for this artifact version, possibly because you don't have access.'")
								.mimeType(MediaType.APPLICATION_JSON_VALUE)
								.build(),
						McpSchema.ResourceTemplate
								.builder("konfigyr://services/{service}/catalog", "service_catalog")
								.title("Service Catalog")
								.description("Full list of configuration properties currently available to the service identified by the " +
										"'service' URI variable (its exact slug, scoped to your namespace), aggregated across its manifest's " +
										"artifacts. This is the complete list; for a targeted lookup by name or keyword instead of the full " +
										"dump, use the search_service_catalog tool.")
								.mimeType(MediaType.APPLICATION_JSON_VALUE)
								.build(),
						McpSchema.ResourceTemplate
								.builder("konfigyr://services/{service}/manifest", "service_manifest")
								.title("Service Manifest")
								.description("Current manifest of the service identified by the 'service' URI variable (its exact slug, " +
										"scoped to your namespace) - the artifact coordinates and versions its latest completed release " +
										"declares. Does not include configuration properties; use the service_catalog resource for those.")
								.mimeType(MediaType.APPLICATION_JSON_VALUE)
								.build()
				);
	}

	@Test
	@DisplayName("should return server info, declared capabilities, and instructions on initialize")
	void shouldInitializeWithMcpScope() {
		final var request = McpSchema.InitializeRequest.builder(
				"2025-06-18",
				McpSchema.ClientCapabilities.builder().build(),
				McpSchema.Implementation.builder("test-client", "1.0.0").build()
		).build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_INITIALIZE, request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result")
				.convertTo(McpSchema.InitializeResult.class)
				.returns("2025-06-18", McpSchema.InitializeResult::protocolVersion)
				.returns(properties.getInstructions(), McpSchema.InitializeResult::instructions)
				.satisfies(it -> assertThat(it.serverInfo())
						.returns(properties.getName(), McpSchema.Implementation::name)
						.returns(properties.getVersion(), McpSchema.Implementation::version)
						.returns(properties.getDescription(), McpSchema.Implementation::description)
						.returns(properties.getWebsiteUrl(), McpSchema.Implementation::websiteUrl)
				)
				.satisfies(it -> assertThat(it.capabilities())
						.returns(McpSchema.ServerCapabilities.ToolCapabilities.builder()
								.listChanged(false)
								.build(), McpSchema.ServerCapabilities::tools)
						.returns(McpSchema.ServerCapabilities.ResourceCapabilities.builder()
								.subscribe(false)
								.listChanged(false)
								.build(), McpSchema.ServerCapabilities::resources)
						.returns(null, McpSchema.ServerCapabilities::prompts)
						.returns(null, McpSchema.ServerCapabilities::completions)
				);
	}

}
