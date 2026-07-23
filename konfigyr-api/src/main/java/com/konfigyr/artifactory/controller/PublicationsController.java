package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.*;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping("/namespaces/{namespace}")
@RequiresScope(OAuthScope.READ_ARTIFACTS)
class PublicationsController {

	private final Artifactory artifactory;
	private final Publications publications;
	private final OwnerResolver ownerResolver;

	@GetMapping("/artifacts")
	@PreAuthorize("isMember(#namespace)")
	PagedModel<EntityModel<ArtifactDefinition>> list(
			@PathVariable String namespace,
			@Nullable @RequestParam(required = false) String groupId,
			@Nullable @RequestParam(required = false) String artifactId,
			@Nullable @RequestParam(required = false) String term,
			Pageable pageable
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		final SearchQuery query = SearchQuery.builder()
				.criteria(ArtifactKey.GROUP_ID_CRITERIA, groupId)
				.criteria(ArtifactKey.ARTIFACT_ID_CRITERIA, artifactId)
				.pageable(pageable)
				.term(term)
				.build();

		return Assemblers.definition(owner).assemble(publications.artifacts(owner, query));
	}

	@GetMapping("/artifacts/{groupId}")
	@PreAuthorize("isMember(#namespace)")
	PagedModel<EntityModel<ArtifactDefinition>> list(
			@PathVariable String namespace,
			@PathVariable String groupId,
			@Nullable @RequestParam(required = false) String term,
			Pageable pageable
	) {
		return list(namespace, groupId, null, term, pageable);
	}

	@GetMapping("/artifacts/{groupId}/{artifactId}")
	@PreAuthorize("isMember(#namespace)")
	EntityModel<ArtifactDefinition> get(
			@PathVariable String namespace,
			@PathVariable String groupId,
			@PathVariable String artifactId
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		final ArtifactKey key = ArtifactKey.of(groupId, artifactId);
		final ArtifactDefinition artifact = publications.get(owner, key)
				.orElseThrow(() -> new ArtifactDefinitionNotFoundException(key));

		return Assemblers.definition(owner).assemble(artifact);
	}

	@RequestMapping(method = RequestMethod.HEAD, path = "/artifacts/{groupId}/{artifactId}")
	@PreAuthorize("isMember(#namespace)")
	ResponseEntity<Void> exists(
			@PathVariable String namespace,
			@PathVariable String groupId,
			@PathVariable String artifactId
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		final ArtifactKey key = ArtifactKey.of(groupId, artifactId);
		final HttpStatus status = publications.exists(owner, key) ? HttpStatus.OK : HttpStatus.NOT_FOUND;

		return ResponseEntity.status(status).build();
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PutMapping("/artifacts/{groupId}/{artifactId}/visibility")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.PUBLISH_ARTIFACTS)
	void changeVisibility(
			@PathVariable String namespace,
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@RequestBody @Validated ChangeVisibilityRequest request
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		publications.changeVisibility(owner, ArtifactKey.of(groupId, artifactId), request.visibility());
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/artifacts/{groupId}/{artifactId}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.PUBLISH_ARTIFACTS)
	void deregister(
			@PathVariable String namespace,
			@PathVariable String groupId,
			@PathVariable String artifactId
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		publications.deregister(owner, ArtifactKey.of(groupId, artifactId));
	}

	@GetMapping("/artifacts/{groupId}/{artifactId}/versions")
	@PreAuthorize("isMember(#namespace)")
	PagedModel<EntityModel<VersionedArtifact>> versions(
			@PathVariable String namespace,
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@Nullable @RequestParam(required = false) String version,
			@Nullable @RequestParam(required = false) String term,
			Pageable pageable
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		final ArtifactKey key = ArtifactKey.of(groupId, artifactId);

		if (!publications.exists(owner, key)) {
			throw new ArtifactDefinitionNotFoundException(key);
		}

		final SearchQuery query = SearchQuery.builder()
				.criteria(ArtifactKey.GROUP_ID_CRITERIA, groupId)
				.criteria(ArtifactKey.ARTIFACT_ID_CRITERIA, artifactId)
				.criteria(ArtifactCoordinates.VERSION_CRITERIA, version)
				.pageable(pageable)
				.term(term)
				.build();

		return Assemblers.artifact(owner).assemble(publications.versions(owner, query));
	}

	@GetMapping("/artifacts/{groupId}/{artifactId}/{version}")
	@PreAuthorize("isMember(#namespace)")
	EntityModel<VersionedArtifact> getVersion(
			@PathVariable String namespace,
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@PathVariable String version
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(groupId, artifactId, version);
		final VersionedArtifact artifact = publications.get(owner, coordinates)
				.orElseThrow(() -> new ArtifactVersionNotFoundException(coordinates));

		return Assemblers.artifact(owner).assemble(artifact);
	}

	@RequestMapping(method = RequestMethod.HEAD, path = "/artifacts/{groupId}/{artifactId}/{version}")
	@PreAuthorize("isMember(#namespace)")
	ResponseEntity<Void> exists(
			@PathVariable String namespace,
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@PathVariable String version
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(groupId, artifactId, version);
		final HttpStatus status = publications.exists(owner, coordinates) ? HttpStatus.OK : HttpStatus.NOT_FOUND;

		return ResponseEntity.status(status).build();
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/artifacts/{groupId}/{artifactId}/{version}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.PUBLISH_ARTIFACTS)
	void retract(
			@PathVariable String namespace,
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@PathVariable String version
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		publications.retract(owner, ArtifactCoordinates.of(groupId, artifactId, version));
	}

	@GetMapping("/artifacts/search")
	@PreAuthorize("isMember(#namespace)")
	PagedModel<EntityModel<PropertyDefinition>> search(
			@PathVariable String namespace,
			@Nullable @RequestParam(required = false) String groupId,
			@Nullable @RequestParam(required = false) String artifactId,
			@Nullable @RequestParam(required = false) String version,
			@Nullable @RequestParam(required = false) String term,
			Pageable pageable
	) {
		final SearchQuery query = SearchQuery.builder()
				.criteria(ArtifactKey.GROUP_ID_CRITERIA, groupId)
				.criteria(ArtifactKey.ARTIFACT_ID_CRITERIA, artifactId)
				.criteria(ArtifactCoordinates.VERSION_CRITERIA, version)
				.pageable(pageable)
				.term(term)
				.build();

		return Assemblers.property().assemble(artifactory.search(ownerResolver.resolve(namespace), query));
	}

	record ChangeVisibilityRequest(@NotNull ArtifactVisibility visibility) { /* noop */ }

}
