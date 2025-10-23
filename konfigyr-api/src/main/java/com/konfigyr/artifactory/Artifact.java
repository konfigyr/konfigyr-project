package com.konfigyr.artifactory;

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
 * Aggregate root representing a software artifact within the Artifactory domain responsible for
 * enforcing the uniqueness constraint on its versions and metadata.
 * <p>
 * An {@code Artifact} represents a uniquely identifiable software component, such as a library, module,
 * or service, identified by its Maven-like coordinates ({@code groupId}, {@code artifactId}). Artifacts
 * act as the conceptual parent for their {@link VersionedArtifact} and the associated
 * {@link PropertyDefinition} entities.
 *
 * @param id entity identifier for the artifact, can't be {@literal null}.
 * @param groupId Maven coordinate {@code groupId} of the artifact, can't be {@literal null}.
 * @param artifactId Maven coordinate {@code artifactId} of the artifact, can't be {@literal null}.
 * @param name human-readable name of the artifact, may be {@literal null}.
 * @param description textual description of the artifact, may be {@literal null}.
 * @param website external URL for documentation or homepage, may be {@literal null}.
 * @param repository source control repository reference (SCM URL), may be {@literal null}.
 * @param createdAt timestamp when was this artifact created, can be {@literal null}.
 * @param updatedAt timestamp when was this artifact last updated, can be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@AggregateRoot
public record Artifact(
	@NonNull @Identity EntityId id,
	@NonNull String groupId,
	@NonNull String artifactId,
	@Nullable String name,
	@Nullable String description,
	@Nullable URI website,
	@Nullable URI repository,
	@Nullable OffsetDateTime createdAt,
	@Nullable OffsetDateTime updatedAt
) implements ArtifactDescriptor, Comparable<Artifact> {

	@Serial
	private static final long serialVersionUID = 8664247652890518668L;

	@Override
	public int compareTo(@NonNull Artifact other) {
		return Comparator.comparing(ArtifactDescriptor::groupId)
				.thenComparing(ArtifactDescriptor::artifactId)
				.compare(this, other);
	}

	/**
	 * Creates a new Artifact builder instance that uses the attributes from the {@link ArtifactDescriptor}.
	 *
	 * @return the artifact builder, never {@literal null}.
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
	 * Creates a new Artifact builder instance.
	 *
	 * @return the artifact builder, never {@literal null}.
	 **/
	@NonNull
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for constructing immutable {@link Artifact} instances.
	 * <p>
	 * The builder enforces explicit intent when creating new domain aggregates. Typically, only the {@code id},
	 * {@code groupId} and {@code artifactId} are mandatory, with other fields being optional.
	 */
	public static final class Builder {
		private EntityId id;
		private String groupId;
		private String artifactId;
		private String name;
		private String description;
		private URI website;
		private URI repository;
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;

		private Builder() {
			// use the static build method...
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link Artifact}.
		 *
		 * @param id internal artifact identifier
		 * @return artifact builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link Artifact}.
		 *
		 * @param id external artifact identifier
		 * @return artifact builder
		 */
		@NonNull
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link Artifact}.
		 *
		 * @param id artifact identifier
		 * @return artifact builder
		 */
		@NonNull
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the {@code groupId} coordinate for this {@link Artifact}.
		 *
		 * @param groupId artifact {@code groupId} coordinate
		 * @return artifact builder
		 */
		@NonNull
		public Builder groupId(String groupId) {
			this.groupId = groupId;
			return this;
		}

		/**
		 * Specify the {@code artifactId} coordinate for this {@link Artifact}.
		 *
		 * @param artifactId artifact {@code artifactId} coordinate
		 * @return artifact builder
		 */
		@NonNull
		public Builder artifactId(String artifactId) {
			this.artifactId = artifactId;
			return this;
		}

		/**
		 * Specify the human-readable name for this {@link Artifact}.
		 *
		 * @param name artifact name
		 * @return artifact builder
		 */
		@NonNull
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Specify the textual description for this {@link Artifact}.
		 *
		 * @param description artifact description
		 * @return artifact builder
		 */
		@NonNull
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Specify an external URL for documentation or homepage for this {@link Artifact}.
		 *
		 * @param website artifact website URL
		 * @return artifact builder
		 * @throws IllegalArgumentException If the given website location violates RFC 2396
		 */
		@NonNull
		public Builder website(String website) {
			return website(website == null ? null : URI.create(website));
		}

		/**
		 * Specify an external URL for documentation or homepage for this {@link Artifact}.
		 *
		 * @param website artifact website URL
		 * @return artifact builder
		 */
		@NonNull
		public Builder website(URI website) {
			this.website = website;
			return this;
		}

		/**
		 * Specify the source control repository location for this {@link Artifact}.
		 *
		 * @param repository artifact repository URL
		 * @return artifact builder
		 */
		@NonNull
		public Builder repository(String repository) {
			return repository(repository == null ? null : URI.create(repository));
		}

		/**
		 * Specify the source control repository location for this {@link Artifact}.
		 *
		 * @param repository artifact repository URL
		 * @return artifact builder
		 */
		@NonNull
		public Builder repository(URI repository) {
			this.repository = repository;
			return this;
		}

		/**
		 * Specify when this {@link Artifact} was created.
		 *
		 * @param createdAt created date
		 * @return artifact builder
		 */
		@NonNull
		public Builder createdAt(Instant createdAt) {
			return createdAt(OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Artifact} was created.
		 *
		 * @param createdAt created date
		 * @return artifact builder
		 */
		@NonNull
		public Builder createdAt(OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link Artifact} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return artifact builder
		 */
		@NonNull
		public Builder updatedAt(Instant updatedAt) {
			return updatedAt(OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Artifact} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return artifact builder
		 */
		@NonNull
		public Builder updatedAt(OffsetDateTime updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		/**
		 * Builds an immutable {@link Artifact} instance.
		 *
		 * @return a new {@link Artifact}, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		public Artifact build() {
			Assert.notNull(id, "Artifact entity identifier can not be null");
			Assert.hasText(groupId, "Artifact groupId can not be null");
			Assert.hasText(artifactId, "Artifact artifactId can not be null");

			return new Artifact(
					id,
					groupId,
					artifactId,
					name,
					description,
					website,
					repository,
					createdAt,
					updatedAt
			);
		}
	}
}
