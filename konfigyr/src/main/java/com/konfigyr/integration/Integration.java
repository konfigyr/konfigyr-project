package com.konfigyr.integration;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record Integration(
		@NonNull EntityId id,
		@NonNull EntityId namespace,
		@NonNull IntegrationType type,
		@NonNull IntegrationProvider provider,
		@Nullable String reference,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1434221962109173821L;

	/**
	 * Creates a new {@link Integration.Builder fluent namespace builder} instance used to create
	 * the {@link Integration} record.
	 *
	 * @return namespace integration builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link Integration Namespace Integration}.
	 */
	public static final class Builder {

		private EntityId id;
		private EntityId namespace;
		private IntegrationType type;
		private IntegrationProvider provider;
		private String reference;
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link Integration}.
		 *
		 * @param id internal namespace integration identifier
		 * @return namespace integration builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link Integration}.
		 *
		 * @param id external namespace integration identifier
		 * @return namespace integration builder
		 */
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link Integration}.
		 *
		 * @param id namespace integration identifier
		 * @return namespace integration builder
		 */
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link Namespace}.
		 *
		 * @param namespace internal namespace identifier
		 * @return namespace integration builder
		 */
		@NonNull
		public Builder namespace(Long namespace) {
			return namespace(EntityId.from(namespace));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link Namespace}.
		 *
		 * @param namespace external namespace identifier
		 * @return namespace integration builder
		 */
		public Builder namespace(String namespace) {
			return namespace(EntityId.from(namespace));
		}

		/**
		 * Specify the {@link EntityId} for this {@link Namespace}.
		 *
		 * @param namespace namespace identifier
		 * @return namespace integration builder
		 */
		public Builder namespace(EntityId namespace) {
			this.namespace = namespace;
			return this;
		}

		/**
		 * The name of the {@link IntegrationType} behind this {@link Integration} that would
		 * be used by the {@link Namespace}.
		 *
		 * @param type integration type name
		 * @return namespace integration builder
		 */
		public Builder type(String type) {
			return StringUtils.hasText(type) ? type(IntegrationType.valueOf(type)) : this;
		}

		/**
		 * The {@link IntegrationType} behind this {@link Integration} that would be used by the {@link Namespace}.
		 *
		 * @param type integration type
		 * @return namespace integration builder
		 */
		public Builder type(IntegrationType type) {
			this.type = type;
			return this;
		}

		/**
		 * The unique provider name that is being integrated for the {@link Namespace}.
		 *
		 * @param provider provider name
		 * @return namespace integration builder
		 */
		public Builder provider(String provider) {
			return StringUtils.hasText(provider) ? provider(IntegrationProvider.valueOf(provider)) : this;
		}

		/**
		 * The integration provider that is being integrated for the {@link Namespace}.
		 *
		 * @param provider integration provider
		 * @return namespace integration builder
		 */
		public Builder provider(IntegrationProvider provider) {
			this.provider = provider;
			return this;
		}

		/**
		 * Certain providers when they are integrated in the {@link Namespace} may contain
		 * a unique identifier, a reference, in their systems. This value defines the link
		 * between the Konfigyr system and the third-party application.
		 *
		 * @param reference provider reference
		 * @return namespace integration builder
		 */
		public Builder reference(String reference) {
			this.reference = reference;
			return this;
		}

		/**
		 * Specify when this {@link Integration} was created.
		 *
		 * @param createdAt created date
		 * @return namespace integration builder
		 */
		public Builder createdAt(Instant createdAt) {
			return createdAt(OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Integration} was created.
		 *
		 * @param createdAt created date
		 * @return namespace integration builder
		 */
		public Builder createdAt(OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link Integration} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return namespace integration builder
		 */
		public Builder updatedAt(Instant updatedAt) {
			return updatedAt(OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Integration} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return namespace integration builder
		 */
		public Builder updatedAt(OffsetDateTime updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		/**
		 * Creates a new instance of the {@link Integration Namespace Integration} using the values defined
		 * in the builder.
		 *
		 * @return namespace integration instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public Integration build() {
			Assert.notNull(id, "Integration entity identifier can not be null");
			Assert.notNull(namespace, "Namespace identifier can not be null for an integration");
			Assert.notNull(type, "Integration type can not be null");
			Assert.notNull(provider, "Integration provider can not be null");

			return new Integration(id, namespace, type, provider, reference, createdAt, updatedAt);
		}

	}

}
