package com.konfigyr.mcp;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.ServiceCatalog;
import com.konfigyr.security.OAuthScope;
import io.modelcontextprotocol.spec.McpSchema;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class McpResourcesTest extends AbstractMcpTest {

	@Autowired
	JsonMapper mapper;

	@Test
	@DisplayName("should retrieve Artifact metadata resource")
	void shouldRetrieveArtifactResource() {
		final var request = McpSchema.ReadResourceRequest.builder("konfigyr://artifacts/com.konfigyr/konfigyr-internal-secrets/1.0.0")
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_ARTIFACTS))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_RESOURCES_READ, request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result")
				.convertTo(McpSchema.ReadResourceResult.class)
				.extracting(McpSchema.ReadResourceResult::contents, InstanceOfAssertFactories.iterable(McpSchema.ResourceContents.class))
				.hasSize(1)
				.first(InstanceOfAssertFactories.type(McpSchema.TextResourceContents.class))
				.returns("konfigyr://artifacts/com.konfigyr/konfigyr-internal-secrets/1.0.0", McpSchema.TextResourceContents::uri)
				.returns(MediaType.APPLICATION_JSON_VALUE, McpSchema.TextResourceContents::mimeType)
				.returns(null, McpSchema.TextResourceContents::meta)
				.extracting(
						contents -> mapper.readValue(contents.text(), ArtifactMetadata.class),
						InstanceOfAssertFactories.type(ArtifactMetadata.class)
				)
				.returns("com.konfigyr", ArtifactDescriptor::groupId)
				.returns("konfigyr-internal-secrets", ArtifactDescriptor::artifactId)
				.returns("1.0.0", Artifact::version)
				.returns("Konfigyr Internal Secrets", ArtifactDescriptor::name)
				.returns("Internal configuration properties, private to the konfigyr namespace", ArtifactDescriptor::description)
				.returns("XwsfowYtnTRZaedC8phWm6ERheuDyC/1a1VI/EnKFt0=", ArtifactMetadata::checksum)
				.extracting(ArtifactMetadata::properties, InstanceOfAssertFactories.iterable(PropertyDescriptor.class))
				.extracting(PropertyDescriptor::name)
				.containsExactlyInAnyOrder("konfigyr.internal.debug-mode", "konfigyr.internal.encryption-key");
	}

	@Test
	@DisplayName("should retrieve unknown Artifact metadata resource")
	void shouldRetrieveUnknownArtifactResource() {
		final var request = McpSchema.ReadResourceRequest.builder("konfigyr://artifacts/com.konfigyr/unknown/1.0.0")
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_ARTIFACTS))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_RESOURCES_READ, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Can not find artifact version with following coordinates: com.konfigyr:unknown:1.0.0")
				));
	}

	@Test
	@DisplayName("should retrieve Artifact metadata resource that is not owned by the current namespace owner")
	void shouldRetrieveArtifactResourceFromDifferentOwner() {
		final var request = McpSchema.ReadResourceRequest.builder("konfigyr://artifacts/doe.john/private-notes/1.0.0")
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_ARTIFACTS))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_RESOURCES_READ, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Can not find artifact version with following coordinates: doe.john:private-notes:1.0.0")
				));
	}

	@Test
	@DisplayName("should retrieve service manifest resource")
	void shouldRetrieveManifestResource() {
		final var request = McpSchema.ReadResourceRequest.builder("konfigyr://services/konfigyr-id/manifest")
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_NAMESPACES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_RESOURCES_READ, request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result")
				.convertTo(McpSchema.ReadResourceResult.class)
				.extracting(McpSchema.ReadResourceResult::contents, InstanceOfAssertFactories.iterable(McpSchema.ResourceContents.class))
				.hasSize(1)
				.first(InstanceOfAssertFactories.type(McpSchema.TextResourceContents.class))
				.returns("konfigyr://services/konfigyr-id/manifest", McpSchema.TextResourceContents::uri)
				.returns(MediaType.APPLICATION_JSON_VALUE, McpSchema.TextResourceContents::mimeType)
				.returns(null, McpSchema.TextResourceContents::meta)
				.extracting(
						contents -> mapper.readValue(contents.text(), Manifest.class),
						InstanceOfAssertFactories.type(Manifest.class)
				)
				.returns(EntityId.from(1).serialize(), Manifest::id)
				.returns("Konfigyr ID", Manifest::name)
				.extracting(Manifest::artifacts, InstanceOfAssertFactories.iterable(ManifestEntry.class))
				.hasSize(8);
	}

	@Test
	@DisplayName("should retrieve manifest resource for an unknown service")
	void shouldRetrieveManifestResourceForUnknownService() {
		final var request = McpSchema.ReadResourceRequest.builder("konfigyr://services/unknown-service/manifest")
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_NAMESPACES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_RESOURCES_READ, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Could not find a service with the following name: unknown-service within a konfigyr Namespace")
				));
	}

	@Test
	@DisplayName("should retrieve manifest resource a service that belongs to a different namespace")
	void shouldRetrieveManifestResourceForOtherService() {
		final var request = McpSchema.ReadResourceRequest.builder("konfigyr://services/john-doe-blog/manifest")
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_NAMESPACES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_RESOURCES_READ, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Could not find a service with the following name: john-doe-blog within a konfigyr Namespace")
				));
	}

	@Test
	@DisplayName("should retrieve service catalog resource")
	void shouldRetrieveCatalogResource() {
		final var request = McpSchema.ReadResourceRequest.builder("konfigyr://services/konfigyr-id/catalog")
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_NAMESPACES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_RESOURCES_READ, request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.result")
				.convertTo(McpSchema.ReadResourceResult.class)
				.extracting(McpSchema.ReadResourceResult::contents, InstanceOfAssertFactories.iterable(McpSchema.ResourceContents.class))
				.hasSize(1)
				.first(InstanceOfAssertFactories.type(McpSchema.TextResourceContents.class))
				.returns("konfigyr://services/konfigyr-id/catalog", McpSchema.TextResourceContents::uri)
				.returns(MediaType.APPLICATION_JSON_VALUE, McpSchema.TextResourceContents::mimeType)
				.returns(null, McpSchema.TextResourceContents::meta)
				.extracting(
						contents -> mapper.readValue(contents.text(), ServiceCatalog.class),
						InstanceOfAssertFactories.type(ServiceCatalog.class)
				)
				.returns(EntityId.from(2), ServiceCatalog::id)
				.returns("latest", ServiceCatalog::version)
				.extracting(ServiceCatalog::properties, InstanceOfAssertFactories.iterable(ServiceCatalog.Property.class))
				.hasSize(4);
	}

	@Test
	@DisplayName("should retrieve service catalog resource for an unknown service")
	void shouldRetrieveCatalogResourceForUnknownService() {
		final var request = McpSchema.ReadResourceRequest.builder("konfigyr://services/unknown-service/catalog")
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_NAMESPACES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_RESOURCES_READ, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Could not find a service with the following name: unknown-service within a konfigyr Namespace")
				));
	}

	@Test
	@DisplayName("should retrieve service catalog resource a service that belongs to a different namespace")
	void shouldRetrieveCatalogResourceForOtherService() {
		final var request = McpSchema.ReadResourceRequest.builder("konfigyr://services/john-doe-blog/catalog")
				.build();

		mvc.post().uri("/mcp")
				.with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_NAMESPACES))
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
				.contentType(MediaType.APPLICATION_JSON)
				.content(serializeMcpRequest(McpSchema.METHOD_RESOURCES_READ, request))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
						.hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
						.hasMessage("Could not find a service with the following name: john-doe-blog within a konfigyr Namespace")
				));
	}

}
