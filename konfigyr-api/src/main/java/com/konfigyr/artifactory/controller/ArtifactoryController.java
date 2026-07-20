package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.*;
import com.konfigyr.hateoas.CollectionModel;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.NamespacedPrincipal;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.version.Version;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NullMarked
@RestController
@RequestMapping("/artifacts")
@RequiredArgsConstructor
@RequiresScope(OAuthScope.READ_ARTIFACTS)
class ArtifactoryController {

	private final Artifactory artifactory;
	private final OwnerResolver resolver;

	@GetMapping("/{groupId}/{artifactId}")
	EntityModel<ArtifactDefinition> definition(
			@PathVariable String groupId,
			@PathVariable String artifactId
	) {
		final ArtifactKey key = ArtifactKey.of(groupId, artifactId);
		final ArtifactDefinition artifact = artifactory.get(resolveOwner().orElse(null), key).orElseThrow(
				() -> new ArtifactDefinitionNotFoundException(key)
		);

		return Assemblers.definition().assemble(artifact);
	}

	@RequestMapping(method = RequestMethod.HEAD, path = "/{groupId}/{artifactId}")
	ResponseEntity<Void> exists(
			@PathVariable String groupId,
			@PathVariable String artifactId
	) {
		final ArtifactKey key = ArtifactKey.of(groupId, artifactId);
		final HttpStatus status = artifactory.exists(resolveOwner().orElse(null), key) ? HttpStatus.OK : HttpStatus.NOT_FOUND;

		return ResponseEntity.status(status).build();
	}

	@GetMapping("/{groupId}/{artifactId}/{version}")
	EntityModel<VersionedArtifact> artifact(
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@PathVariable String version
	) {
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(groupId, artifactId, version);
		final VersionedArtifact artifact = artifactory.get(resolveOwner().orElse(null), coordinates).orElseThrow(
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
		final HttpStatus status = artifactory.exists(resolveOwner().orElse(null), coordinates) ? HttpStatus.OK : HttpStatus.NOT_FOUND;

		return ResponseEntity.status(status).build();
	}

	@PostMapping("/{groupId}/{artifactId}/{version}")
	@RequiresScope(OAuthScope.PUBLISH_ARTIFACTS)
	EntityModel<Publication> publish(
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@PathVariable String version,
			@RequestBody @Validated ArtifactPublication publication,
			BindingResult errors
	) throws BindException {
		final Owner owner = resolveOwner().orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
				"Could not extract namespace identifier from the current authenticated principal"
		));

		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(groupId, artifactId, version);
		publication.validate(coordinates, errors);

		return EntityModel.of(artifactory.publish(owner, publication.toArtifactMetadata()));
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

	@PutMapping("/{groupId}/{artifactId}/visibility")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.PUBLISH_ARTIFACTS)
	void changeVisibility(
			@PathVariable String groupId,
			@PathVariable String artifactId,
			@RequestBody @Validated ChangeVisibilityRequest request
	) {
		final Owner owner = resolveOwner().orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
				"Could not extract namespace identifier from the current authenticated principal"
		));

		artifactory.changeVisibility(owner, ArtifactKey.of(groupId, artifactId), request.visibility());
	}

	private Optional<Owner> resolveOwner() {
		final AuthenticatedPrincipal principal = AuthenticatedPrincipal.resolve();
		if (principal instanceof NamespacedPrincipal namespacedPrincipal) {
			return namespacedPrincipal.getNamespaceId()
					.map(resolver::resolve);
		}
		return Optional.empty();
	}

	@NullUnmarked
	record ArtifactPublication(
			@NotBlank String groupId,
			@NotBlank String artifactId,
			@NotNull Version version,
			String name,
			String description,
			URI website,
			URI repository,
			@NotBlank String checksum,
			@Valid @NotEmpty List<@NotNull ArtifactPublicationProperty> properties
	) {

		void validate(ArtifactCoordinates coordinates, BindingResult errors) throws BindException {
			if (StringUtils.hasText(groupId()) && !Objects.equals(coordinates.groupId(), groupId)) {
				reject(errors, "groupId", coordinates.groupId(), groupId());
			}
			if (StringUtils.hasText(artifactId()) && !Objects.equals(coordinates.artifactId(), artifactId)) {
				reject(errors, "artifactId", coordinates.artifactId(), artifactId());
			}
			if (version() != null && !Objects.equals(coordinates.version(), version())) {
				reject(errors, "version", coordinates.version().get(), version().get());
			}
			if (errors.hasErrors()) {
				throw new BindException(errors);
			}
		}

		ArtifactMetadata toArtifactMetadata() {
			final var builder = ArtifactMetadata.builder()
					.groupId(groupId)
					.artifactId(artifactId)
					.version(version.get())
					.name(name)
					.description(description)
					.website(website)
					.repository(repository)
					.checksum(checksum);

			for (final ArtifactPublicationProperty property : properties) {
				builder.property(property.toPropertyDescriptor());
			}

			return builder.build();
		}

		private void reject(Errors errors, String field, String expected, String actual) {
			final String defaultMessage = "The field '%s' of artifact metadata must match '%s' but was: '%s'"
					.formatted(field, expected, actual);

			errors.rejectValue(field, "artifactory.validation.metadata." + field + ".mismatch", defaultMessage);
		}

	}

	@NullUnmarked
	record ArtifactPublicationProperty(
			@NotBlank String name,
			@NotNull JsonSchema schema,
			@NotBlank String typeName,
			String description,
			String defaultValue,
			Deprecation deprecation
	) {

		PropertyDescriptor toPropertyDescriptor() {
			return PropertyDescriptor.builder()
					.name(name)
					.schema(schema)
					.typeName(typeName)
					.description(description)
					.defaultValue(defaultValue)
					.deprecation(deprecation)
					.build();
		}

	}

	@NullUnmarked
	record ChangeVisibilityRequest(@NotNull ArtifactVisibility visibility) { /* noop */ }
}
