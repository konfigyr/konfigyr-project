package com.konfigyr.mcp;

import com.konfigyr.entity.EntityId;
import com.konfigyr.security.KonfigyrClaimNames;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestAccounts;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.assertj.core.api.ThrowingConsumer;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

abstract class AbstractMcpTest extends AbstractControllerTest {

	@Autowired
	protected McpJsonMapper mapper;

	protected byte[] serializeMcpRequest(String method, Object params) {
		return serializeMcpRequest(new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, method,
				UUID.randomUUID().toString(), params));
	}

	protected byte[] serializeMcpRequest(McpSchema.JSONRPCRequest request) {
		try {
			return mapper.writeValueAsBytes(request);
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to serialize MCP request: " + request, ex);
		}
	}

	/**
	 * Creates an authentication post-processor whose access token carries the {@code namespace} claim,
	 * so the reading principal resolves to the given namespace.
	 *
	 * @param namespace the namespace identifier to add as a JWT claim, can't be {@literal null}
	 * @param scopes scopes to add as a JWT claim, can't be {@literal null}
	 * @return the authentication post-processor, never {@literal null}
	 */
	static RequestPostProcessor authentication(EntityId namespace, OAuthScope... scopes) {
		return authentication(claims -> claims
				.subject(TestAccounts.jane().build().id().serialize())
				.claim(KonfigyrClaimNames.NAMESPACE, namespace.serialize())
				.claim(OAuth2ParameterNames.SCOPE, OAuthScopes.of(scopes).toString()));
	}

	/**
	 * Creates consumer that can be used to assert the {@link McpSchema.JSONRPCResponse.JSONRPCError}
	 * that is extracted from {@link MvcTestResult}.
	 * <p>
	 * This method would also assert the following values from the {@link MvcTestResult}:
	 * <ul>
	 *     <li>The HTTP status code should match the given {@link HttpStatusCode}</li>
	 *     <li>The HTTP response content type should match {@link MediaType#APPLICATION_JSON}</li>
	 * </ul>
	 *
	 * @param statusCode expected status code, can't be {@literal null}
	 * @param consumer consumer function to assert {@link McpSchema.JSONRPCResponse.JSONRPCError}
	 * @return the JSON RPC error consumer
	 */
	protected static ThrowingConsumer<MvcTestResult> mcpErrorFor(
			@NonNull HttpStatusCode statusCode,
			@NonNull ThrowingConsumer<JSONRPCErrorAssert> consumer
	) {
		return result -> consumer.accept(
				result.assertThat()
						.hasStatus(statusCode.value())
						.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
						.bodyJson()
						.extractingPath("$.error")
						.convertTo(JSONRPCErrorAssert.factory())
		);
	}

}
