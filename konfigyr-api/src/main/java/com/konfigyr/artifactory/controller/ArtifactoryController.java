package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.CollectionModel;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.NamespacedPrincipal;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@NullMarked
@RestController
@RequestMapping("/artifacts")
@RequiredArgsConstructor
class ArtifactoryController {

	private final Artifactory artifactory;

	@GetMapping("/{groupId}/{artifactId}/{version}")
	EntityModel<VersionedArtifact> artifact(
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@PathVariable String version
	) {
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(groupId, artifactId, version);
		final VersionedArtifact artifact = artifactory.get(coordinates).orElseThrow(
				() -> new ArtifactVersionNotFoundException(coordinates)
		);

		return Assemblers.artifact(coordinates).assemble(artifact);
	}

	@RequestMapping(method = RequestMethod.HEAD, path = "/{groupId}/{artifactId}/{version}")
	ResponseEntity<Void> exists(
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@PathVariable String version
	) {
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(groupId, artifactId, version);
		final HttpStatus status = artifactory.exists(coordinates) ? HttpStatus.OK : HttpStatus.NOT_FOUND;

		return ResponseEntity.status(status).build();
	}

	@PostMapping("/{groupId}/{artifactId}/{version}")
	EntityModel<Release> release(
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@PathVariable String version,
			@RequestBody DefaultArtifactMetadata metadata,
			BindingResult errors
	) throws BindException {
		final EntityId namespaceId = retrieveNamespaceIdentifier()
				.orElseThrow(() -> new ArtifactoryException(HttpStatus.BAD_REQUEST, "Namespace id is not available for current principal"));

		final ArtifactMetadataValidator validator = new ArtifactMetadataValidator(groupId, artifactId, version);
		validator.validate(metadata, errors);

		return EntityModel.of(artifactory.release(namespaceId, metadata));
	}

	@GetMapping("/{groupId}/{artifactId}/{version}/properties")
	CollectionModel<EntityModel<PropertyDefinition>> properties(
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@PathVariable String version
	) {
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(groupId, artifactId, version);
		return Assemblers.property(coordinates).assemble(artifactory.properties(coordinates));
	}

	static Optional<EntityId> retrieveNamespaceIdentifier() {
		final AuthenticatedPrincipal principal = AuthenticatedPrincipal.resolve();

		if (principal instanceof NamespacedPrincipal namespacedPrincipal) {
			return namespacedPrincipal.getNamespaceId();
		}

		return Optional.empty();
	}
}
