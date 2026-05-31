package com.konfigyr.kms;

import com.konfigyr.crypto.KeyStatus;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Represents the metadata of a Key inside a Keyset managed by the KMS.
 *
 * @param id The unique identifier of the key metadata, can't be {@literal null}.
 * @param algorithm The algorithm used by the key, can't be {@literal null}.
 * @param status The current state of the key, can't be {@literal null}.
 * @param isPrimary Whether the key is the primary key for the keyset.
 * @param createdAt When the key was created, can be {@literal null}.
 * @param initializedAt When the key was initialized, can be {@literal null}.
 * @param expiresAt When the key will expire, can be {@literal null}.
 * @param destructionScheduledAt When the key was scheduled for destruction, can be {@literal null}.
 * @param destroyedAt When the key was destroyed, can be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Entity
public record KeyMetadata(
		@NonNull @Identity String id,
		@NonNull KeysetMetadataAlgorithm algorithm,
		@NonNull KeyStatus status,
		boolean isPrimary,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime initializedAt,
		@Nullable OffsetDateTime expiresAt,
		@Nullable OffsetDateTime destructionScheduledAt,
		@Nullable OffsetDateTime destroyedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 7253920426969048947L;

	/**
	 * Creates a new {@link Builder fluent builder} instance used to create the {@link KeyMetadata} record.
	 *
	 * @return key metadata builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link KeyMetadata}.
	 */
	public static final class Builder {
		private String id;
		private KeysetMetadataAlgorithm algorithm;
		private KeyStatus status;
		private boolean isPrimary;
		private OffsetDateTime createdAt;
		private OffsetDateTime initializedAt;
		private OffsetDateTime expiresAt;
		private OffsetDateTime destructionScheduledAt;
		private OffsetDateTime destroyedAt;

		private Builder() {
		}

		/**
		 * Specify the identifier for this {@link KeyMetadata}.
		 *
		 * @param id key metadata identifier
		 * @return key metadata builder
		 */
		@NonNull
		public Builder id(@Nullable String id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the name of algorithm that is used by this {@link KeyMetadata}.
		 *
		 * @param algorithm algorithm name
		 * @return key metadata builder
		 */
		public Builder algorithm(@Nullable KeysetMetadataAlgorithm algorithm) {
			this.algorithm = algorithm;
			return this;
		}

		/**
		 * Specify the status of this {@link KeyMetadata}.
		 *
		 * @param status key metadata status
		 * @return key metadata builder
		 */
		public Builder status(@Nullable KeyStatus status) {
			this.status = status;
			return this;
		}

		/**
		 * Specify whether this {@link KeyMetadata} represents the primary key.
		 *
		 * @param isPrimary primary key flag
		 * @return key metadata builder
		 */
		public Builder isPrimary(boolean isPrimary) {
			this.isPrimary = isPrimary;
			return this;
		}

		/**
		 * Specify when this {@link KeyMetadata} was created.
		 *
		 * @param createdAt created date
		 * @return key metadata builder
		 */
		public Builder createdAt(@Nullable Long createdAt) {
			return createdAt(createdAt == null ? null : Instant.ofEpochMilli(createdAt));
		}

		/**
		 * Specify when this {@link KeyMetadata} was created.
		 *
		 * @param createdAt created date
		 * @return key metadata builder
		 */
		public Builder createdAt(@Nullable Instant createdAt) {
			return createdAt(createdAt == null ? null : createdAt.atOffset(ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link KeyMetadata} was created.
		 *
		 * @param createdAt created date
		 * @return key metadata builder
		 */
		public Builder createdAt(@Nullable OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link KeyMetadata} was initialized.
		 *
		 * @param initializedAt initialized date
		 * @return key metadata builder
		 */
		public Builder initializedAt(@Nullable Long initializedAt) {
			return initializedAt(initializedAt == null ? null : Instant.ofEpochMilli(initializedAt));
		}

		/**
		 * Specify when this {@link KeyMetadata} was initialized.
		 *
		 * @param initializedAt initialized date
		 * @return key metadata builder
		 */
		public Builder initializedAt(@Nullable Instant initializedAt) {
			return initializedAt(initializedAt == null ? null : initializedAt.atOffset(ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link KeyMetadata} was initialized.
		 *
		 * @param initializedAt initialized date
		 * @return key metadata builder
		 */
		public Builder initializedAt(@Nullable OffsetDateTime initializedAt) {
			this.initializedAt = initializedAt;
			return this;
		}

		/**
		 * Specify when this {@link KeyMetadata} will expire.
		 *
		 * @param expiresAt expiration date
		 * @return key metadata builder
		 */
		public Builder expiresAt(@Nullable Long expiresAt) {
			return expiresAt(expiresAt == null ? null : Instant.ofEpochMilli(expiresAt));
		}

		/**
		 * Specify when this {@link KeyMetadata} will expire.
		 *
		 * @param expiresAt expiration date
		 * @return key metadata builder
		 */
		public Builder expiresAt(@Nullable Instant expiresAt) {
			return expiresAt(expiresAt == null ? null : expiresAt.atOffset(ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link KeyMetadata} will expire.
		 *
		 * @param expiresAt expiration date
		 * @return key metadata builder
		 */
		public Builder expiresAt(@Nullable OffsetDateTime expiresAt) {
			this.expiresAt = expiresAt;
			return this;
		}

		/**
		 * Specify when this {@link KeyMetadata} was scheduled for destruction.
		 *
		 * @param destructionScheduledAt destruction scheduled date
		 * @return key metadata builder
		 */
		public Builder destructionScheduledAt(@Nullable Long destructionScheduledAt) {
			return destructionScheduledAt(destructionScheduledAt == null ? null : Instant.ofEpochMilli(destructionScheduledAt));
		}

		/**
		 * Specify when this {@link KeyMetadata} was scheduled for destruction.
		 *
		 * @param destructionScheduledAt destruction scheduled date
		 * @return key metadata builder
		 */
		public Builder destructionScheduledAt(@Nullable Instant destructionScheduledAt) {
			return destructionScheduledAt(destructionScheduledAt == null ? null : destructionScheduledAt.atOffset(ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link KeyMetadata} was scheduled for destruction.
		 *
		 * @param destructionScheduledAt destruction scheduled date
		 * @return key metadata builder
		 */
		public Builder destructionScheduledAt(@Nullable OffsetDateTime destructionScheduledAt) {
			this.destructionScheduledAt = destructionScheduledAt;
			return this;
		}

		/**
		 * Specify when this {@link KeyMetadata} was destroyed.
		 *
		 * @param destroyedAt destroyed date
		 * @return key metadata builder
		 */
		public Builder destroyedAt(@Nullable Long destroyedAt) {
			return destroyedAt(destroyedAt == null ? null : Instant.ofEpochMilli(destroyedAt));
		}

		/**
		 * Specify when this {@link KeyMetadata} was destroyed.
		 *
		 * @param destroyedAt destroyed date
		 * @return key metadata builder
		 */
		public Builder destroyedAt(@Nullable Instant destroyedAt) {
			return destroyedAt(destroyedAt == null ? null : destroyedAt.atOffset(ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link KeyMetadata} was destroyed.
		 *
		 * @param destroyedAt destroyed date
		 * @return key metadata builder
		 */
		public Builder destroyedAt(@Nullable OffsetDateTime destroyedAt) {
			this.destroyedAt = destroyedAt;
			return this;
		}

		/**
		 * Creates a new instance of the {@link KeyMetadata} using the values defined in the builder.
		 *
		 * @return key metadata instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public KeyMetadata build() {
			Assert.notNull(id, "Key metadata identifier can not be null");
			Assert.notNull(algorithm, "Key metadata algorithm can not be null");
			Assert.notNull(status, "Key metadata status can not be null");

			return new KeyMetadata(id, algorithm, status, isPrimary, createdAt, initializedAt,
					expiresAt, destructionScheduledAt, destroyedAt);
		}
	}

}
