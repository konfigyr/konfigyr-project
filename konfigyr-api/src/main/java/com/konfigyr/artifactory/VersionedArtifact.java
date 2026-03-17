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
 * Each {@code VersionedArtifact} represents a single release or build of an {@link ArtifactDefinition},
 * identified by its semantic version (e.g., {@code 1.0.0}) and a content checksum.
 * <p>
 * In DDD terms, it belongs to the {@link ArtifactDefinition} aggregate and does not exist
 * independently — its lifecycle is governed by its parent artifact.
 * </p>
 *
 * @param id entity identifier for the artifact version, can't be {@literal null}.
 * @param artifact entity identifier for the artifact, can't be {@literal null}.
 * @param coordinates Maven coordinates of the artifact, can't be {@literal null}.
 * @param name human-readable name of the artifact, may be {@literal null}.
 * @param description textual description of the artifact, may be {@literal null}.
 * @param state current release state of the artifact version, can't be {@literal null}.
 * @param checksum checksum that uniquely identifying the contents of this release, may be {@literal null}.
 * @param website external URL for documentation or homepage, may be {@literal null}.
 * @param repository source control repository reference (SCM URL), may be {@literal null}.
 * @param releasedAt timestamp when was this artifact version released, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@AggregateRoot
public record VersionedArtifact(
		@NonNull @Identity EntityId id,
		@NonNull @Association(aggregateType = ArtifactDefinition.class) EntityId artifact,
		@NonNull ArtifactCoordinates coordinates,
		@NonNull ReleaseState state,
		@Nullable String checksum,
		@Nullable String name,
		@Nullable String description,
		@Nullable URI website,
		@Nullable URI repository,
		@NonNull Instant releasedAt
) implements ArtifactDescriptor, Release {

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
	public static final class Builder extends ReleaseBuilder<VersionedArtifact, Builder> {
		private EntityId id;
		private EntityId artifact;

		private Builder() {
			// use the builder static method
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
		 * Specify when this {@link VersionedArtifact} was released.
		 *
		 * @param releasedAt release date
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder releasedAt(OffsetDateTime releasedAt) {
			return releasedAt(releasedAt == null ? null : releasedAt.toInstant());
		}

		/**
		 * Builds an immutable {@link VersionedArtifact} instance.
		 *
		 * @return a new {@link VersionedArtifact}, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		@Override
		public VersionedArtifact build() {
			Assert.notNull(id, "Versioned artifact entity identifier can not be null");
			Assert.notNull(artifact, "Artifact entity identifier can not be null");
			Assert.hasText(groupId, "Artifact groupId can not be null");
			Assert.hasText(artifactId, "Artifact artifactId can not be null");
			Assert.notNull(version, "Artifact version can not be null");
			Assert.notNull(releasedAt, "Created date can not be null");

			return new VersionedArtifact(id, artifact, ArtifactCoordinates.of(groupId, artifactId, version),
					state == null ? ReleaseState.PENDING : state, checksum, name, description, website,
					repository, releasedAt);
		}
	}

}
