package com.konfigyr.namespace.controller;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.NamespaceNotFoundException;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceNotFoundException;
import com.konfigyr.namespace.Services;
import com.konfigyr.namespace.manifest.ServiceManifests;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/namespaces/{namespace}/services/{slug}")
class ServiceManifestController {

	private final NamespaceManager namespaces;
	private final Services services;
	private final ServiceManifests manifests;

	@GetMapping("manifest")
	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	EntityModel<Manifest> manifest(@PathVariable @NonNull String namespace, @PathVariable @NonNull String slug) {
		final Namespace ns = lookupNamespace(namespace);
		final Service service = services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		);

		return Assemblers.manifest(ns, service).assemble(manifests.get(service));
	}

	@PostMapping("releases")
	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.PUBLISH_MANIFESTS)
	EntityModel<ServiceRelease> resolve(
			@PathVariable String namespace,
			@PathVariable String slug,
			@RequestBody List<ServiceReleaseCandidate> candidates
	) {
		final Namespace ns = lookupNamespace(namespace);
		final Service service = services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		);

		return Assemblers.release(ns, service).assemble(manifests.open(service, candidates));
	}

	@PostMapping("/releases/{id}/artifacts")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.PUBLISH_MANIFESTS)
	void upload(
			@PathVariable String namespace,
			@PathVariable String slug,
			@PathVariable EntityId id,
			@RequestBody ArtifactMetadata metadata
	) {
		final Namespace ns = lookupNamespace(namespace);
		final Service service = services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		);

		manifests.upload(service, id, metadata);
	}

	@PostMapping("/releases/{id}/complete")
	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.PUBLISH_MANIFESTS)
	ResponseEntity<EntityModel<ServiceRelease>> complete(
			@PathVariable String namespace,
			@PathVariable String slug,
			@PathVariable EntityId id
	) {
		final Namespace ns = lookupNamespace(namespace);
		final Service service = services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		);

		final ServiceRelease release = manifests.complete(service, id);
		final HttpStatus status = release.state() == ReleaseState.FAILED ? HttpStatus.CONFLICT : HttpStatus.OK;

		return ResponseEntity.status(status)
				.body(Assemblers.release(ns, service).assemble(release));
	}

	@NonNull
	Namespace lookupNamespace(@NonNull String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

}
