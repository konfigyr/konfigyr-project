package com.konfigyr.kms;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Record used to define a {@link KeysetMetadata} that would be created by the KMS.
 *
 * @param algorithm The algorithm used by the keyset, can't be {@literal null}.
 * @param name The name of the keyset, can't be {@literal null}.
 * @param description The description of the keyset, can be {@literal null}.
 * @param tags The tags associated with the keyset, can't be {@literal null}.
 * @param rotationInterval The rotation interval for the keyset, can't be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see KeysetMetadata
 */
@NullMarked
@ValueObject
public record KeysetMetadataDefinition(
		KeysetMetadataAlgorithm algorithm,
		String name,
		@Nullable String description,
		Set<String> tags,
		Duration rotationInterval
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 283753676517870624L;

	/**
	 * Creates a new {@link Builder fluent builder} instance used to create the {@link KeysetMetadataDefinition} record.
	 *
	 * @return keyset metadata definition builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link KeysetMetadataDefinition}.
	 */
	public static final class Builder {
		@Nullable private KeysetMetadataAlgorithm algorithm;
		@Nullable private String name;
		@Nullable private String description;
		@Nullable private Duration rotationInterval;
		private final Set<String> tags;

		private Builder() {
			this.tags = new LinkedHashSet<>();
		}

		/**
		 * Specify the algorithm that is used by this {@link KeysetMetadata}.
		 *
		 * @param algorithm keyset metadata algorithm
		 * @return keyset metadata definition builder
		 */
		public Builder algorithm(@Nullable KeysetMetadataAlgorithm algorithm) {
			this.algorithm = algorithm;
			return this;
		}


		/**
		 * Specify the name of this {@link KeysetMetadataDefinition}.
		 *
		 * @param name keyset name
		 * @return keyset metadata definition builder
		 */
		public Builder name(@Nullable String name) {
			this.name = name;
			return this;
		}

		/**
		 * Describe this {@link KeysetMetadataDefinition}.
		 *
		 * @param description keyset namespace description
		 * @return keyset metadata definition builder
		 */
		public Builder description(@Nullable String description) {
			this.description = description;
			return this;
		}

		/**
		 * Adds a tag to this {@link KeysetMetadataDefinition}.
		 *
		 * @param tag tag to be added
		 * @return keyset metadata definition builder
		 */
		public Builder tag(@Nullable String tag) {
			if (StringUtils.hasText(tag)) {
				this.tags.add(tag);
			}
			return this;
		}

		/**
		 * Adds tags to this {@link KeysetMetadataDefinition}.
		 *
		 * @param tags tag to be added
		 * @return keyset metadata definition builder
		 */
		public Builder tags(@Nullable Iterable<String> tags) {
			if (tags != null) {
				tags.forEach(this::tag);
			}
			return this;
		}

		/**
		 * Adds a rotation interval to this {@link KeysetMetadataDefinition}.
		 *
		 * @param rotationInterval rotation interval for the keyset metadata
		 * @return keyset metadata definition builder
		 */
		public Builder rotationInterval(@Nullable Duration rotationInterval) {
			this.rotationInterval = rotationInterval;
			return this;
		}

		/**
		 * Creates a new instance of the {@link KeysetMetadataDefinition} using the values defined in the builder.
		 *
		 * @return keyset metadata definition instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		public KeysetMetadataDefinition build() {
			Assert.notNull(algorithm, "Keyset metadata algorithm can not be null");
			Assert.notNull(name, "Keyset metadata name can not be null");

			return new KeysetMetadataDefinition(algorithm, name, description, Collections.unmodifiableSet(tags),
					rotationInterval == null ? Duration.ofDays(180) : rotationInterval);
		}
	}

}
