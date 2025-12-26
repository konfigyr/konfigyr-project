package com.konfigyr.kms;

import com.konfigyr.crypto.Algorithm;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents the metadata of a Keyset managed by the KMS.
 *
 * @param id The unique identifier of the keyset metadata, can't be {@literal null}.
 * @param algorithm The algorithm used by the keyset, can't be {@literal null}.
 * @param state The current state of the keyset, can't be {@literal null}.
 * @param name The name of the keyset, can't be {@literal null}.
 * @param description The description of the keyset, can be {@literal null}.
 * @param tags The tags associated with the keyset, can't be {@literal null}.
 * @param createdAt When the keyset was created, can be {@literal null}.
 * @param updatedAt When the keyset was last updated, can be {@literal null}.
 * @param destroyedAt When the keyset was destroyed, can be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@AggregateRoot
public record KeysetMetadata(
		@NonNull @Identity EntityId id,
		@NonNull String algorithm,
		@NonNull KeysetMetadataState state,
		@NonNull String name,
		@Nullable String description,
		@NonNull Set<String> tags,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt,
		@Nullable OffsetDateTime destroyedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 62775127006730468L;

	/**
	 * The {@link SearchQuery.Criteria} descriptor used to narrow down the search of {@link KeysetMetadata}
	 * by their {@link EntityId entity identifier}.
	 */
	public static final SearchQuery.Criteria<EntityId> ID_CRITERIA =
			SearchQuery.criteria("kms-keyset-metadata.id", EntityId.class);

	/**
	 * The {@link SearchQuery.Criteria} descriptor used to narrow down the search of {@link KeysetMetadata}
	 * by their algorithm.
	 */
	public static final SearchQuery.Criteria<String> ALGORITHM_CRITERIA =
			SearchQuery.criteria("kms-keyset-metadata.algorithm", String.class);

	/**
	 * The {@link SearchQuery.Criteria} descriptor used to narrow down the search of {@link KeysetMetadata}
	 * by their state.
	 */
	public static final SearchQuery.Criteria<KeysetMetadataState> STATE_CRITERIA =
			SearchQuery.criteria("kms-keyset-metadata.state", KeysetMetadataState.class);

	/**
	 * Creates a new {@link Builder fluent builder} instance used to create the {@link KeysetMetadata} record.
	 *
	 * @return keyset metadata builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link KeysetMetadata}.
	 */
	public static final class Builder {
		private EntityId id;
		private String algorithm;
		private KeysetMetadataState state;
		private String name;
		private String description;
		private final Set<String> tags;
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;
		private OffsetDateTime destroyedAt;

		private Builder() {
			this.tags = new LinkedHashSet<>();
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link KeysetMetadata}.
		 *
		 * @param id internal keyset metadata identifier
		 * @return keyset metadata builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link KeysetMetadata}.
		 *
		 * @param id external keyset metadata identifier
		 * @return keyset metadata builder
		 */
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link KeysetMetadata}.
		 *
		 * @param id keyset metadata identifier
		 * @return keyset metadata builder
		 */
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the algorithm that is used by this {@link KeysetMetadata}.
		 *
		 * @param algorithm algorithm name
		 * @return keyset metadata builder
		 */
		public Builder algorithm(Algorithm algorithm) {
			return algorithm(algorithm == null ? null : algorithm.name());
		}

		/**
		 * Specify the name of algorithm that is used by this {@link KeysetMetadata}.
		 *
		 * @param algorithm algorithm name
		 * @return keyset metadata builder
		 */
		public Builder algorithm(String algorithm) {
			this.algorithm = algorithm;
			return this;
		}

		/**
		 * Specify the state of this {@link KeysetMetadata}.
		 *
		 * @param state keyset metadata state
		 * @return keyset metadata builder
		 */
		public Builder state(KeysetMetadataState state) {
			this.state = state;
			return this;
		}

		/**
		 * Specify the name of this {@link KeysetMetadata}.
		 *
		 * @param name keyset name
		 * @return keyset metadata builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Describe this {@link KeysetMetadata}.
		 *
		 * @param description keyset namespace description
		 * @return keyset metadata builder
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Adds a tag to this {@link KeysetMetadata}.
		 *
		 * @param tag tag to be added
		 * @return keyset metadata builder
		 */
		public Builder tag(String tag) {
			if (StringUtils.hasText(tag)) {
				this.tags.add(tag);
			}
			return this;
		}

		/**
		 * Adds tags to this {@link KeysetMetadata}.
		 *
		 * @param tags tag to be added
		 * @return keyset metadata builder
		 */
		public Builder tags(Iterable<String> tags) {
			if (tags != null) {
				tags.forEach(this::tag);
			}
			return this;
		}

		/**
		 * Specify when this {@link KeysetMetadata} was created.
		 *
		 * @param createdAt created date
		 * @return keyset metadata builder
		 */
		public Builder createdAt(Instant createdAt) {
			return createdAt(OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link KeysetMetadata} was created.
		 *
		 * @param createdAt created date
		 * @return keyset metadata builder
		 */
		public Builder createdAt(OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link KeysetMetadata} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return keyset metadata builder
		 */
		public Builder updatedAt(Instant updatedAt) {
			return updatedAt(OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link KeysetMetadata} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return keyset metadata builder
		 */
		public Builder updatedAt(OffsetDateTime updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		/**
		 * Specify when this {@link KeysetMetadata} was destroyed.
		 *
		 * @param destroyedAt destroyed date
		 * @return keyset metadata builder
		 */
		public Builder destroyedAt(Instant destroyedAt) {
			return destroyedAt(OffsetDateTime.ofInstant(destroyedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link KeysetMetadata} was destroyed.
		 *
		 * @param destroyedAt destroyed date
		 * @return keyset metadata builder
		 */
		public Builder destroyedAt(OffsetDateTime destroyedAt) {
			this.destroyedAt = destroyedAt;
			return this;
		}

		/**
		 * Creates a new instance of the {@link KeysetMetadata} using the values defined in the builder.
		 *
		 * @return keyset metadata instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public KeysetMetadata build() {
			Assert.notNull(id, "Keyset metadata entity identifier can not be null");
			Assert.notNull(algorithm, "Keyset metadata algorithm can not be null");
			Assert.notNull(state, "Keyset metadata state can not be null");
			Assert.notNull(name, "Keyset metadata name can not be null");

			return new KeysetMetadata(id, algorithm, state, name, description, Collections.unmodifiableSet(tags),
					createdAt, updatedAt, destroyedAt);
		}
	}

}
