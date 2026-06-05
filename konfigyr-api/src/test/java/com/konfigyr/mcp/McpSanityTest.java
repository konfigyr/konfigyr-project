package com.konfigyr.mcp;

import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.KeyGenerator;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end sanity tests that drive the full MCP protocol against a real HTTP server
 * using a real OAuth2 Bearer token, replicating how an MCP client such as an IDE
 * extension or an AI agent would connect to Konfigyr in production.
 * <p>
 * The test:
 * <ol>
 *     <li>Starts the full Spring Boot application on a random port</li>
 *     <li>Signs JWT tokens with the {@link KeyGenerator} RSA key and the WireMock issuer URL</li>
 *     <li>Registers the corresponding JWKS endpoint on WireMock so the resource server can
 *         validate the tokens</li>
 *     <li>Creates an {@link McpSyncClient} with an {@code Authorization: Bearer <token>} header
 *         and connects via HTTP to {@code /mcp}</li>
 *     <li>Runs the full MCP handshake: {@code initialize} → {@code tools/list} → {@code tools/call}</li>
 * </ol>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpSanityTest extends AbstractControllerTest {

	@LocalServerPort
	int port;

	@Test
	@DisplayName("Anonymous MCP client is rejected before the MCP handshake begins")
	void anonymousClientIsRejected() {
		try (var client = mcpClient(null)) {
			assertThatThrownBy(client::initialize)
					.as("Spring Security must reject the SSE connection with 401")
					.isInstanceOf(Exception.class);
		}
	}

	@Test
	@DisplayName("Authenticated MCP client can initialize, discover tools, and invoke helloWorld")
	void authenticatedClientCanInvokeHelloWorld() {
		final String token = generateAccessToken(builder -> builder
				.subject("kfg-A2c7mvoxEP1rb-_NQLvaZ5KJNTGR-oOp")
				.claim(StandardClaimNames.EMAIL, "sanity@konfigyr.com")
				.claim(OAuth2ParameterNames.SCOPE, OAuthScope.READ_NAMESPACES.getAuthority())
		);

		try (var client = mcpClient(token)) {
			final var session = client.initialize();

			assertThat(session.serverInfo())
					.returns("konfigyr-mcp-server", McpSchema.Implementation::name)
					.returns("1.0.0", McpSchema.Implementation::version);

			final var tools = client.listTools().tools();

			assertThat(tools)
					.satisfiesExactlyInAnyOrder(
							tool -> McpToolAssert.assertThat(tool)
									.hasName("searchProperties")
									.descriptionContains("Search for Spring Boot configuration property descriptors")
									.hasInputSchemaProperty("query")
									.hasInputSchemaProperty("service")
									.hasInputSchemaProperty("artifacts")
									.hasInputSchemaProperty("includeDeprecated")
									.hasInputSchemaProperty("limit")
									.hasRequiredInputProperty("query")
					);

			assertSearchProperties(client);
		}
	}

	void assertSearchProperties(McpSyncClient client) {
		final var result = client.callTool(
				McpSchema.CallToolRequest.builder("searchProperties").arguments(Map.of(
						"query", "resource bundle",
						"artifacts", "org.springframework.boot:spring-boot-autoconfigure:4.0.4",
						"includeDeprecated", false,
						"limit", 10
				)).build()
		);

		assertThat(result.isError())
				.as("Tool invocation must succeed without errors")
				.isFalse();

		assertThat(result.content())
				.hasSize(1)
				.first()
				.asInstanceOf(InstanceOfAssertFactories.type(McpSchema.TextContent.class))
				.extracting(McpSchema.TextContent::text)
				.asString()
				.contains("\"name\":\"spring.messages.cache-duration\"")
				.contains("\"checksum\":\"L00K3chGss7gZbW+gC7ONlfarxA2JoYoUo0yHkhvuCI=\"")
				.contains("Loaded resource bundle files cache duration");
	}

	/**
	 * Creates an {@link McpSyncClient} that connects to the test server's SSE endpoint.
	 * <p>
	 * When {@code bearerToken} is non-null it is added as {@code Authorization: Bearer <token>}
	 * to every outgoing HTTP request (both the SSE connection and later message POSTs).
	 *
	 * @param bearerToken the bearer token to be used for authentication, can be {@literal null}
	 *                    to simulate an unauthenticated client.
	 */
	private McpSyncClient mcpClient(@Nullable String bearerToken) {
		final HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
				.builder("http://localhost:" + port)
				.openConnectionOnStartup(false)
				.resumableStreams(false)
				.httpRequestCustomizer((builder, _, _, _, _) -> {
					if (bearerToken != null) {
						builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
					}
				})
				.build();

		return McpClient.sync(transport)
				.requestTimeout(Duration.ofSeconds(10000))
				.build();
	}


}
