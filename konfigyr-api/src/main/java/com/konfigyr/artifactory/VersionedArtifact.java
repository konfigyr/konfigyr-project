package com.konfigyr.artifactory;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Value object representing a specific version of an {@link ArtifactDefinition}.
 * <p>
 * Each {@code VersionedArtifact} represents a single publication or build of an {@link ArtifactDefinition},
 * identified by its semantic version (e.g., {@code 1.0.0}) and a content checksum.
 * <p>
 * In DDD terms, it belongs to the {@link ArtifactDefinition} aggregate and does not exist
 * independently — its lifecycle is governed by its parent artifact.
 * </p>
 *
 * @param id entity identifier for the artifact version, can't be {@literal null}.
 * @param artifact entity identifier for the artifact, can't be {@literal null}.
 * @param owner the namespace that owns this artifact, can't be {@literal null}.
 * @param coordinates Maven coordinates of the artifact, can't be {@literal null}.
 * @param visibility whether this artifact is readable by every namespace or only its owner, can't be {@literal null}.
 * @param name human-readable name of the artifact, may be {@literal null}.
 * @param description textual description of the artifact, may be {@literal null}.
 * @param state current publication state of the artifact version, can't be {@literal null}.
 * @param checksum checksum that uniquely identifying the contents of this publication, may be {@literal null}.
 * @param website external URL for documentation or homepage, may be {@literal null}.
 * @param repository source control repository reference (SCM URL), may be {@literal null}.
 * @param publishedAt timestamp when was this artifact version published, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@AggregateRoot
public record VersionedArtifact(
		@NonNull @Identity EntityId id,
		@NonNull @Association(aggregateType = ArtifactDefinition.class) EntityId artifact,
		@NonNull Owner owner,
		@NonNull ArtifactCoordinates coordinates,
		@NonNull ArtifactVisibility visibility,
		@NonNull PublicationState state,
		@Nullable String checksum,
		@Nullable String name,
		@Nullable String description,
		@Nullable URI website,
		@Nullable URI repository,
		@NonNull Instant publishedAt
) implements ArtifactKey, ArtifactDescriptor, Publication {

	@Serial
	private static final long serialVersionUID = 2501993329087351789L;

	@Override
	@JsonGetter
	public @NonNull String groupId() {
		return coordinates.groupId();
	}

	@Override
	@JsonGetter
	public @NonNull String artifactId() {
		return coordinates.artifactId();
	}

	@Override
	@JsonGetter
	public @NonNull String version() {
		return coordinates.version().get();
	}

	@Override
	@JsonGetter
	public @NonNull List<String> errors() {
		return Collections.emptyList();
	}

	/**
	 * Creates a new {@link VersionedArtifact} builder instance that uses the attributes from the
	 * {@link ArtifactDescriptor}.
	 *
	 * @param descriptor the artifact descriptor to copy the attributes from, can't be {@literal null}.
	 * @return the versioned artifact builder, never {@literal null}.
	 **/
	@NonNull
	public static Builder from(@NonNull ArtifactDescriptor descriptor) {
		return builder()
				.groupId(descriptor.groupId())
				.artifactId(descriptor.artifactId())
				.name(descriptor.name())
				.description(descriptor.description())
				.website(descriptor.website())
				.repository(descriptor.repository());
	}

	/**
	 * Creates a new {@link VersionedArtifact} builder instance that uses the attributes from the
	 * {@link Artifact}.
	 *
	 * @param artifact the artifact to copy the attributes from, can't be {@literal null}.
	 * @return the versioned artifact builder, never {@literal null}.
	 **/
	@NonNull
	public static Builder from(@NonNull Artifact artifact) {
		return from((ArtifactDescriptor) artifact)
				.version(artifact.version());
	}

	/**
	 * Creates a new {@link VersionedArtifact} builder instance that uses the attributes from the
	 * {@link ArtifactDefinition}, including its {@link Owner} and {@link ArtifactVisibility}.
	 *
	 * @param definition the artifact definition to copy the attributes from, can't be {@literal null}.
	 * @return the versioned artifact builder, never {@literal null}.
	 **/
	@NonNull
	public static Builder from(@NonNull ArtifactDefinition definition) {
		return from((ArtifactDescriptor) definition)
				.owner(definition.owner())
				.visibility(definition.visibility());
	}

	/**
	 * Creates a new {@link VersionedArtifact} builder instance.
	 *
	 * @return the versioned artifact builder, never {@literal null}.
	 **/
	@NonNull
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for constructing immutable {@link VersionedArtifact} instances.
	 * <p>
	 * The builder enforces explicit intent when creating new domain aggregates. Typically, only the {@code id},
	 * {@code groupId}, {@code artifactId} and {@code version} are mandatory, with other fields being optional.
	 */
	public static final class Builder extends PublicationBuilder<VersionedArtifact, Builder> {
		private EntityId id;
		private EntityId artifact;
		private Owner owner;
		private ArtifactVisibility visibility;

		private Builder() {
			// use the builder static method
		}

		/**
		 * Specify the {@link Owner} namespace for this {@link VersionedArtifact}.
		 *
		 * @param owner the owning namespace
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder owner(Owner owner) {
			this.owner = owner;
			return this;
		}

		/**
		 * Specify whether this {@link VersionedArtifact} is readable by every namespace or only its
		 * owner. Defaults to {@link ArtifactVisibility#PRIVATE} when not specified.
		 *
		 * @param visibility artifact visibility
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder visibility(ArtifactVisibility visibility) {
			this.visibility = visibility;
			return this;
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link VersionedArtifact}.
		 *
		 * @param id internal versioned artifact identifier
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link VersionedArtifact}.
		 *
		 * @param id external versioned artifact identifier
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link VersionedArtifact}.
		 *
		 * @param id versioned artifact identifier
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the internal {@link EntityId} for the {@link ArtifactDefinition} that
		 * owns this {@link VersionedArtifact}.
		 *
		 * @param artifact internal artifact identifier
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder artifact(Long artifact) {
			return artifact(EntityId.from(artifact));
		}

		/**
		 * Specify the external {@link EntityId} for the {@link ArtifactDefinition} that
		 * owns this {@link VersionedArtifact}.
		 *
		 * @param artifact external artifact identifier
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder artifact(String artifact) {
			return artifact(EntityId.from(artifact));
		}

		/**
		 * Specify the {@link EntityId} for the {@link ArtifactDefinition} that owns this {@link VersionedArtifact}.
		 *
		 * @param artifact artifact identifier
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder artifact(EntityId artifact) {
			this.artifact = artifact;
			return this;
		}

		/**
		 * Specify when this {@link VersionedArtifact} was published.
		 *
		 * @param publishedAt publish date
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder publishedAt(OffsetDateTime publishedAt) {
			return this.publishedAt(publishedAt.toInstant());
		}

		/**
		 * Builds an immutable {@link VersionedArtifact} instance.
		 *
		 * @return a new {@link VersionedArtifact}, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		@Override
		public VersionedArtifact instantiate() {
			Assert.notNull(id, "Versioned artifact entity identifier can not be null");
			Assert.notNull(artifact, "Artifact entity identifier can not be null");
			Assert.notNull(owner, "Artifact owner can not be null");
			Assert.hasText(groupId, "Artifact groupId can not be null");
			Assert.hasText(artifactId, "Artifact artifactId can not be null");
			Assert.notNull(version, "Artifact version can not be null");
			Assert.notNull(publishedAt, "Created date can not be null");

			return new VersionedArtifact(id, artifact, owner, ArtifactCoordinates.of(groupId, artifactId, version),
					visibility == null ? ArtifactVisibility.PRIVATE : visibility,
					state == null ? PublicationState.PENDING : state, checksum, name, description, website,
					repository, publishedAt);
		}
	}

}
