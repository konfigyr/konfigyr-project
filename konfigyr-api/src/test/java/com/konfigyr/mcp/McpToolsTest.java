package com.konfigyr.mcp;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.vault.ChangeRequest;
import com.konfigyr.vault.ChangeRequestMergeStatus;
import com.konfigyr.vault.ChangeRequestState;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfilePolicy;
import io.modelcontextprotocol.spec.McpSchema;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class McpToolsTest extends AbstractMcpTest {

	@Test
	@DisplayName("should list services within the current namespace")
	void shouldListServices() {
		final var request = McpSchema.CallToolRequest.builder("list_services").build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_NAMESPACES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result.structuredContent.contents")
				.convertTo(InstanceOfAssertFactories.list(Service.class))
				.extracting(Service::slug, Service::name)
				.containsExactlyInAnyOrder(
						tuple("konfigyr-id", "Konfigyr ID"),
						tuple("konfigyr-api", "Konfigyr API")
				);
	}

	@Test
	@DisplayName("should require the READ_NAMESPACES scope to list services")
	void shouldRequireScopeToListServices() {
		final var request = McpSchema.CallToolRequest.builder("list_services").build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Access Denied")
				));
	}

	@Test
	@DisplayName("should list a service's profiles")
	void shouldListProfiles() {
		final var request = McpSchema.CallToolRequest.builder("list_profiles")
				.arguments(Map.of("service", "konfigyr-id"))
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_PROFILES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result.structuredContent.contents")
				.convertTo(InstanceOfAssertFactories.list(Profile.class))
				.extracting(Profile::slug, Profile::name, Profile::policy)
				.containsExactlyInAnyOrder(
						tuple("development", "Development", ProfilePolicy.UNPROTECTED),
						tuple("staging", "Staging", ProfilePolicy.PROTECTED),
						tuple("production", "Prod", ProfilePolicy.PROTECTED),
						tuple("locked", "QA", ProfilePolicy.IMMUTABLE)
				);
	}

	@Test
	@DisplayName("should fail to list profiles for an unknown service")
	void shouldListProfilesForUnknownService() {
		final var request = McpSchema.CallToolRequest.builder("list_profiles")
				.arguments(Map.of("service", "unknown-service"))
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_PROFILES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Could not find a service with the following name: unknown-service within a konfigyr Namespace")
				));
	}

	@Test
	@DisplayName("should fail to list profiles for a service that belongs to a different namespace")
	void shouldListProfilesForServiceInDifferentNamespace() {
		final var request = McpSchema.CallToolRequest.builder("list_profiles")
				.arguments(Map.of("service", "john-doe-blog"))
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_PROFILES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Could not find a service with the following name: john-doe-blog within a konfigyr Namespace")
				));
	}

	@Test
	@DisplayName("should require the READ_PROFILES scope to list a service's profiles")
	void shouldRequireScopeToListProfiles() {
		final var request = McpSchema.CallToolRequest.builder("list_profiles")
				.arguments(Map.of("service", "konfigyr-id"))
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Access Denied")
				));
	}

	@Test
	@DisplayName("should list open change requests for a service by default")
	void shouldListOpenChangeRequestsByDefault() {
		final var request = McpSchema.CallToolRequest.builder("list_change_requests")
				.arguments(Map.of("service", "konfigyr-id"))
				.build();

		assertThatChangeRequests(request)
				.extracting(ChangeRequest::number, ChangeRequest::state, ChangeRequest::subject)
				.containsExactlyInAnyOrder(
						tuple(2L, ChangeRequestState.OPEN, "Increase server port"),
						tuple(3L, ChangeRequestState.OPEN, "Update datasource URL"),
						tuple(4L, ChangeRequestState.OPEN, "Tune logging levels")
				);
	}

	@Test
	@DisplayName("should list change requests filtered by state")
	void shouldListChangeRequestsFilteredByState() {
		final var request = McpSchema.CallToolRequest.builder("list_change_requests")
				.arguments(Map.of("service", "konfigyr-id", "state", "MERGED"))
				.build();

		assertThatChangeRequests(request)
				.extracting(ChangeRequest::number, ChangeRequest::state, ChangeRequest::subject)
				.containsExactly(tuple(1L, ChangeRequestState.MERGED, "Update application name"));
	}

	@Test
	@DisplayName("should list change requests filtered by profile")
	void shouldListChangeRequestsFilteredByProfile() {
		final var request = McpSchema.CallToolRequest.builder("list_change_requests")
				.arguments(Map.of("service", "konfigyr-id", "profile", "production"))
				.build();

		assertThatChangeRequests(request)
				.extracting(ChangeRequest::number, ChangeRequest::state, ChangeRequest::subject)
				.containsExactly(tuple(4L, ChangeRequestState.OPEN, "Tune logging levels"));
	}

	@Test
	@DisplayName("should require the READ_PROFILES scope to list change requests")
	void shouldRequireScopeToListChangeRequests() {
		final var request = McpSchema.CallToolRequest.builder("list_change_requests")
				.arguments(Map.of("service", "konfigyr-id"))
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Access Denied")
				));
	}

	@Test
	@DisplayName("should fail to list change requests for a service that belongs to a different namespace")
	void shouldListChangeRequestsForServiceInDifferentNamespace() {
		final var request = McpSchema.CallToolRequest.builder("list_change_requests")
				.arguments(Map.of("service", "john-doe-blog"))
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_PROFILES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Could not find a service with the following name: john-doe-blog within a konfigyr Namespace")
				));
	}

	@Test
	@DisplayName("should retrieve a single change request by its number")
	void shouldRetrieveChangeRequest() {
		final var request = McpSchema.CallToolRequest.builder("get_change_request")
				.arguments(Map.of("service", "konfigyr-id", "number", 2))
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.PROFILES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result.structuredContent")
				.convertTo(ChangeRequest.class)
				.returns(2L, ChangeRequest::number)
				.returns(ChangeRequestState.OPEN, ChangeRequest::state)
				.returns(ChangeRequestMergeStatus.MERGEABLE, ChangeRequest::mergeStatus)
				.returns("Increase server port", ChangeRequest::subject)
				.returns(2, ChangeRequest::count)
				.returns("John Doe", ChangeRequest::createdBy)
				.satisfies(changeRequest -> assertThat(changeRequest.profile().slug()).isEqualTo("locked"))
				.satisfies(changeRequest -> assertThat(changeRequest.service().slug()).isEqualTo("konfigyr-id"));
	}

	@Test
	@DisplayName("should require the READ_PROFILES scope to retrieve a change request")
	void shouldRequireScopeToRetrieveChangeRequest() {
		final var request = McpSchema.CallToolRequest.builder("get_change_request")
				.arguments(Map.of("service", "konfigyr-id", "number", 2))
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Access Denied")
				));
	}

	@Test
	@DisplayName("should fail to retrieve a change request for a service that belongs to a different namespace")
	void shouldRetrieveChangeRequestForServiceInDifferentNamespace() {
		final var request = McpSchema.CallToolRequest.builder("get_change_request")
				.arguments(Map.of("service", "john-doe-blog", "number", 1))
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_PROFILES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Could not find a service with the following name: john-doe-blog within a konfigyr Namespace")
				));
	}

	private ListAssert<ChangeRequest> assertThatChangeRequests(McpSchema.CallToolRequest request) {
		return mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_PROFILES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result.structuredContent.contents")
				.convertTo(InstanceOfAssertFactories.list(ChangeRequest.class));
	}

}
