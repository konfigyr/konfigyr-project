package com.konfigyr.mcp;

import com.konfigyr.artifactory.*;
import com.konfigyr.mcp.annotation.McpComponent;
import com.konfigyr.mcp.annotation.McpResource;
import com.konfigyr.mcp.annotation.McpTemplateVariable;
import com.konfigyr.namespace.*;
import com.konfigyr.namespace.catalog.ServiceCatalogSource;
import com.konfigyr.namespace.manifest.ServiceManifests;
import com.konfigyr.security.KonfigyrClaimNames;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import io.modelcontextprotocol.common.McpTransportContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;

@McpComponent
@RequiredArgsConstructor
class McpResources {

	private final Artifactory artifactory;
	private final Services services;
	private final ServiceManifests manifests;
	private final ServiceCatalogSource catalogs;

	@McpResource(
			uri = "konfigyr://services/{service}/manifest",
			name = "service_manifest",
			title = "Service Manifest",
			description = "Current manifest of the service identified by the 'service' URI variable (its exact slug, " +
					"scoped to your namespace) - the artifact coordinates and versions its latest completed release " +
					"declares. Does not include configuration properties; use the service_catalog resource for those.",
			mimeType = MediaType.APPLICATION_JSON_VALUE
	)
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	Manifest serviceManifest(McpTransportContext context, @McpTemplateVariable("service") String slug) {
		final Namespace namespace = (Namespace) context.get(KonfigyrClaimNames.NAMESPACE);
		final Service service = services.get(namespace, slug)
				.orElseThrow(() -> new ServiceNotFoundException(namespace.slug(), slug));

		return manifests.get(service);
	}

	@McpResource(
			uri = "konfigyr://services/{service}/catalog",
			name = "service_catalog",
			title = "Service Catalog",
			description = "Full list of configuration properties currently available to the service identified by the " +
					"'service' URI variable (its exact slug, scoped to your namespace), aggregated across its manifest's " +
					"artifacts. This is the complete list; for a targeted lookup by name or keyword instead of the full " +
					"dump, use the search_service_catalog tool.",
			mimeType = MediaType.APPLICATION_JSON_VALUE
	)
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	ServiceCatalog serviceCatalog(McpTransportContext context, @McpTemplateVariable("service") String slug) {
		final Namespace namespace = (Namespace) context.get(KonfigyrClaimNames.NAMESPACE);
		final Service service = services.get(namespace, slug)
				.orElseThrow(() -> new ServiceNotFoundException(namespace.slug(), slug));

		return catalogs.get(service);
	}

	@McpResource(
			uri = "konfigyr://artifacts/{groupId}/{artifactId}/{version}",
			name = "artifact_metadata",
			title = "Artifact metadata",
			description = "Full property list for one exact artifact version, identified by the 'groupId', 'artifactId', " +
					"and 'version' URI variables (standard Maven-style coordinates). A not-found result (unknown/unpublished " +
					"coordinates, or a private artifact outside your namespace) is a normal empty read, not an error - " +
					"treat it as 'no properties known for this artifact version, possibly because you don't have access.'",
			mimeType = MediaType.APPLICATION_JSON_VALUE
	)
	@RequiresScope(OAuthScope.READ_ARTIFACTS)
	ArtifactMetadata artifact(
			McpTransportContext context,
			@McpTemplateVariable String groupId,
			@McpTemplateVariable String artifactId,
			@McpTemplateVariable String version
	) {
		final Namespace namespace = (Namespace) context.get(KonfigyrClaimNames.NAMESPACE);
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(groupId, artifactId, version);

		final VersionedArtifact artifact = artifactory.get(new Owner(namespace.id(), namespace.slug()), coordinates)
				.orElseThrow(() -> new ArtifactVersionNotFoundException(coordinates));

		return artifact.toMetadata(artifactory.properties(coordinates));
	}

}
