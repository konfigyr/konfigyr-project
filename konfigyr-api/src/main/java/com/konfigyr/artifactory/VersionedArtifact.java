package com.konfigyr.artifactory;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;

/**
 * Value object representing a specific version of an {@link Artifact}.
 * <p>
 * Each {@code VersionedArtifact} represents a single release or build of an {@link Artifact},
 * identified by its semantic version (e.g., {@code 1.0.0}) and a content checksum.
 * <p>
 * In DDD terms, it belongs to the {@link Artifact} aggregate and does not exist
 * independently — its lifecycle is governed by its parent artifact.
 * </p>
 *
 * @param id entity identifier for the artifact version, can't be {@literal null}.
 * @param groupId Maven coordinate {@code groupId} of the artifact, can't be {@literal null}.
 * @param artifactId Maven coordinate {@code artifactId} of the artifact, can't be {@literal null}.
 * @param version Maven coordinate {@code version} of the artifact, can't be {@literal null}.
 * @param name human-readable name of the artifact, may be {@literal null}.
 * @param description textual description of the artifact, may be {@literal null}.
 * @param checksum checksum that uniquely identifying the contents of this release, may be {@literal null}.
 * @param website external URL for documentation or homepage, may be {@literal null}.
 * @param repository source control repository reference (SCM URL), may be {@literal null}.
 * @param releasedAt timestamp when was this artifact version released, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@AggregateRoot
public record VersionedArtifact (
		@NonNull @Identity EntityId id,
		@NonNull String groupId,
		@NonNull String artifactId,
		@NonNull Version version,
		@Nullable String checksum,
		@Nullable String name,
		@Nullable String description,
		@Nullable URI website,
		@Nullable URI repository,
		@Nullable OffsetDateTime releasedAt
) implements ArtifactDescriptor, ArtifactCoordinates {

	@Serial
	private static final long serialVersionUID = 2501993329087351789L;

	@Override
	public int compareTo(@NonNull ArtifactCoordinates other) {
		return Comparator.comparing(ArtifactCoordinates::groupId)
				.thenComparing(ArtifactCoordinates::artifactId)
				.thenComparing(ArtifactCoordinates::version)
				.compare(this, other);
	}

	/**
	 * Creates a new {@link VersionedArtifact} builder instance that uses the attributes from the
	 * {@link ArtifactDescriptor}.
	 *
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
	public static final class Builder {
		private EntityId id;
		private String groupId;
		private String artifactId;
		private Version version;
		private String checksum;
		private String name;
		private String description;
		private URI website;
		private URI repository;
		private OffsetDateTime releasedAt;

		private Builder() {}

		/**
		 * Specify the internal {@link EntityId} for this {@link VersionedArtifact}.
		 *
		 * @param id internal artifact identifier
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link VersionedArtifact}.
		 *
		 * @param id external artifact identifier
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link VersionedArtifact}.
		 *
		 * @param id artifact identifier
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the {@code groupId} coordinate for this {@link VersionedArtifact}.
		 *
		 * @param groupId artifact {@code groupId} coordinate
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder groupId(String groupId) {
			this.groupId = groupId;
			return this;
		}

		/**
		 * Specify the {@code artifactId} coordinate for this {@link VersionedArtifact}.
		 *
		 * @param artifactId artifact {@code artifactId} coordinate
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder artifactId(String artifactId) {
			this.artifactId = artifactId;
			return this;
		}

		/**
		 * Specify the {@code version} coordinate for this {@link VersionedArtifact}.
		 *
		 * @param version artifact {@code version} coordinate
		 * @return versioned artifact builder
		 * @throws ParseException when the supplied artifact version is invalid
		 */
		@NonNull
		public Builder version(String version) {
			return version(version == null ? null : Version.valueOf(version));
		}

		/**
		 * Specify the {@code version} coordinate for this {@link VersionedArtifact}.
		 *
		 * @param version artifact {@code version} coordinate
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder version(Version version) {
			this.version = version;
			return this;
		}

		/**
		 * Specify the checksum that uniquely identifying the contents of this {@link VersionedArtifact}.
		 *
		 * @param checksum release checksum
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder checksum(String checksum) {
			this.checksum = checksum;
			return this;
		}

		/**
		 * Specify the human-readable name for this {@link VersionedArtifact}.
		 *
		 * @param name artifact name
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Specify the textual description for this {@link VersionedArtifact}.
		 *
		 * @param description artifact description
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Specify an external URL for documentation or homepage for this {@link VersionedArtifact}.
		 *
		 * @param website artifact website URL
		 * @return versioned artifact builder
		 * @throws IllegalArgumentException If the given website location violates RFC 2396
		 */
		@NonNull
		public Builder website(String website) {
			return website(website == null ? null : URI.create(website));
		}

		/**
		 * Specify an external URL for documentation or homepage for this {@link VersionedArtifact}.
		 *
		 * @param website artifact website URL
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder website(URI website) {
			this.website = website;
			return this;
		}

		/**
		 * Specify the source control repository location for this {@link VersionedArtifact}.
		 *
		 * @param repository artifact repository URL
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder repository(String repository) {
			return repository(repository == null ? null : URI.create(repository));
		}

		/**
		 * Specify the source control repository location for this {@link VersionedArtifact}.
		 *
		 * @param repository artifact repository URL
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder repository(URI repository) {
			this.repository = repository;
			return this;
		}

		/**
		 * Specify when this {@link VersionedArtifact} was released.
		 *
		 * @param releasedAt release date
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder releasedAt(Instant releasedAt) {
			return releasedAt(OffsetDateTime.ofInstant(releasedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link VersionedArtifact} was released.
		 *
		 * @param releasedAt release date
		 * @return versioned artifact builder
		 */
		@NonNull
		public Builder releasedAt(OffsetDateTime releasedAt) {
			this.releasedAt = releasedAt;
			return this;
		}

		/**
		 * Builds an immutable {@link VersionedArtifact} instance.
		 *
		 * @return a new {@link VersionedArtifact}, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		public VersionedArtifact build() {
			Assert.notNull(id, "Artifact entity identifier can not be null");
			Assert.hasText(groupId, "Artifact groupId can not be null");
			Assert.hasText(artifactId, "Artifact artifactId can not be null");
			Assert.notNull(version, "Artifact version can not be null");

			return new VersionedArtifact(
					id,
					groupId,
					artifactId,
					version,
					checksum,
					name,
					description,
					website,
					repository,
					releasedAt
			);
		}
	}
	
}
