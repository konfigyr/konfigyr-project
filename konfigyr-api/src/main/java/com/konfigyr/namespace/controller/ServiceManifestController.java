package com.konfigyr.namespace.controller;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceNotFoundException;
import com.konfigyr.namespace.Services;
import com.konfigyr.namespace.manifest.ReleaseNotFoundException;
import com.konfigyr.namespace.manifest.ServiceManifests;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.NamespacedPrincipal;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller used by build plugins to resolve, upload artifacts to, and complete a {@link Service}
 * release. Endpoints are namespace-free: the owning {@link Namespace} is resolved from the namespace
 * claim carried by the authenticated namespace client's OAuth2 token rather than a URL path variable.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/releases/{service}")
class ServiceManifestController {

	private final NamespaceManager namespaces;
	private final Services services;
	private final ServiceManifests manifests;

	@PostMapping
	@RequiresScope(OAuthScope.PUBLISH_RELEASES)
	EntityModel<ServiceRelease> resolve(
			@PathVariable String service,
			@RequestBody List<ServiceReleaseCandidate> candidates
	) {
		final Service svc = lookupService(service);

		return Assemblers.release(svc).assemble(manifests.open(svc, candidates));
	}

	@GetMapping("/{id}")
	@RequiresScope(OAuthScope.PUBLISH_RELEASES)
	EntityModel<ServiceRelease> release(
			@PathVariable String service,
			@PathVariable EntityId id
	) {
		final Service svc = lookupService(service);

		final ServiceRelease release = manifests.get(svc, id).orElseThrow(
				() -> new ReleaseNotFoundException(id)
		);

		return Assemblers.release(svc).assemble(release);
	}

	@PostMapping("/{id}/artifacts")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.PUBLISH_RELEASES)
	void upload(
			@PathVariable String service,
			@PathVariable EntityId id,
			@RequestBody ArtifactMetadata metadata
	) {
		manifests.upload(lookupService(service), id, metadata);
	}

	@PostMapping("/{id}/complete")
	@RequiresScope(OAuthScope.PUBLISH_RELEASES)
	ResponseEntity<EntityModel<ServiceRelease>> complete(
			@PathVariable String service,
			@PathVariable EntityId id
	) {
		final Service svc = lookupService(service);

		final ServiceRelease release = manifests.complete(svc, id);
		final HttpStatus status = release.state() == ReleaseState.FAILED ? HttpStatus.CONFLICT : HttpStatus.OK;

		return ResponseEntity.status(status)
				.body(Assemblers.release(svc).assemble(release));
	}

	@NonNull
	private Service lookupService(@NonNull String slug) {
		final Namespace namespace = currentNamespace();

		return services.get(namespace, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace.slug(), slug)
		);
	}

	/**
	 * Resolves the {@link Namespace} that owns the authenticated namespace client, using the
	 * namespace claim carried by its OAuth2 token rather than a URL path variable.
	 * <p>
	 * Any failure to establish this namespace context is treated as an authorization failure rather
	 * than a not-found or server error: a token that reaches this endpoint without a resolvable
	 * namespace claim is not entitled to act on any namespace, regardless of the reason.
	 */
	@NonNull
	private Namespace currentNamespace() {
		final AuthenticatedPrincipal principal = AuthenticatedPrincipal.resolve();

		if (!(principal instanceof NamespacedPrincipal namespaced)) {
			throw new AccessDeniedException("Authenticated principal is not scoped to a namespace");
		}

		final EntityId id = namespaced.getNamespaceId().orElseThrow(
				() -> new AccessDeniedException("Authenticated principal is missing its namespace claim")
		);

		return namespaces.findById(id).orElseThrow(
				() -> new AccessDeniedException("Authenticated principal's namespace claim does not match a known namespace")
		);
	}

}
