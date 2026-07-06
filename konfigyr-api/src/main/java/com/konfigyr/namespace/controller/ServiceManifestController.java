package com.konfigyr.namespace.controller;

import com.konfigyr.artifactory.ArtifactMetadata;
import com.konfigyr.artifactory.ServiceRelease;
import com.konfigyr.artifactory.ServiceReleaseCandidate;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.NamespaceNotFoundException;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceNotFoundException;
import com.konfigyr.namespace.Services;
import com.konfigyr.namespace.manifest.ServiceManifests;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequiresScope(OAuthScope.PUBLISH_MANIFESTS)
@RequestMapping("/namespaces/{namespace}/services/{slug}/releases")
class ServiceManifestController {

	private final NamespaceManager namespaces;
	private final Services services;
	private final ServiceManifests manifests;

	@PostMapping
	@PreAuthorize("isMember(#namespace)")
	ServiceRelease resolve(
			@PathVariable String namespace,
			@PathVariable String slug,
			@RequestBody @Validated ResolveReleaseRequest request
	) {
		final Namespace ns = lookupNamespace(namespace);
		final Service service = services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		);

		return manifests.open(service, request.artifacts());
	}

	@PostMapping("/{id}/artifacts")
	@PreAuthorize("isMember(#namespace)")
	void upload(
			@PathVariable String namespace,
			@PathVariable String slug,
			@PathVariable String id,
			@RequestBody ArtifactMetadata metadata
	) {
		final Namespace ns = lookupNamespace(namespace);
		final Service service = services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		);

		manifests.upload(service, EntityId.from(id), metadata);
	}

	@NonNull
	Namespace lookupNamespace(@NonNull String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	record ResolveReleaseRequest(@NotNull List<@NotNull ServiceReleaseCandidate> artifacts) {

	}

}
