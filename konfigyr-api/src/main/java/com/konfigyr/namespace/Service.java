package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Represents a deployable Spring Boot application unit within a {@link Namespace}.
 * <p>
 * A service corresponds to a single Spring Boot application or microservice that belongs to a
 * specific {@link Namespace}. It acts as the anchor entity for all configuration, observability,
 * and access control features associated with that application.
 * <p>
 * Each service is uniquely identified by its {@code id} and scoped within a namespace through
 * {@code namespaceId}. The {@code slug} provides a human-readable, URL-safe identifier suitable for
 * referencing services in APIs or UI routes.
 * <p>
 * In Domain-Driven Design terms, {@code Service} is an aggregate root within the {@code namespace} bounded
 * context. Other modules may reference this entity but do not own it.
 *
 * @param id unique identifier of this service, can't be {@literal null}
 * @param namespace unique identifier of the namespace, can't be {@literal null}
 * @param slug unique service identifier derived from its name, can't be {@literal null}
 * @param name human friendly name of the service, can't be {@literal null}
 * @param description short description of the service, can be {@literal null}
 * @param createdAt when was this service created, can be {@literal null}
 * @param updatedAt when was this service last updated, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@AggregateRoot
public record Service(
		@NonNull @Identity EntityId id,
		@NonNull @Association(aggregateType = Namespace.class) EntityId namespace,
		@NonNull String slug,
		@NonNull String name,
		@Nullable String description,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 5109729066973698023L;

	/**
	 * Creates a new {@link Builder fluent namespace builder} instance used to create
	 * the {@link Service} record.
	 *
	 * @return service builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create a {@link Service}.
	 */
	public static final class Builder {

		private EntityId id;
		private EntityId namespace;
		private String slug;
		private String name;
		private String description;
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link Service}.
		 *
		 * @param id internal service identifier
		 * @return service builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link Service}.
		 *
		 * @param id internal service identifier
		 * @return service builder
		 */
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link Service}.
		 *
		 * @param id service identifier
		 * @return service builder
		 */
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the internal {@link EntityId} for the {@link Namespace} that owns this {@link Service}.
		 *
		 * @param id internal namespace identifier
		 * @return service builder
		 */
		@NonNull
		public Builder namespace(Long id) {
			return namespace(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for the {@link Namespace} that owns this {@link Service}.
		 *
		 * @param id internal namespace identifier
		 * @return service builder
		 */
		public Builder namespace(String id) {
			return namespace(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for the {@link Namespace} that owns this {@link Service}.
		 *
		 * @param id namespace identifier
		 * @return service builder
		 */
		public Builder namespace(EntityId id) {
			this.namespace = id;
			return this;
		}

		/**
		 * Specify the slug that identifies this {@link Service}.
		 *
		 * @param slug service slug
		 * @return service builder
		 */
		public Builder slug(String slug) {
			this.slug = slug;
			return this;
		}

		/**
		 * Specify the user-friendly name for this {@link Service}.
		 *
		 * @param name service name
		 * @return service builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Describe this {@link Service}.
		 *
		 * @param description service description
		 * @return service builder
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Specify when this {@link Service} was created.
		 *
		 * @param createdAt created date
		 * @return service builder
		 */
		public Builder createdAt(Instant createdAt) {
			return createdAt(OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Service} was created.
		 *
		 * @param createdAt created date
		 * @return service builder
		 */
		public Builder createdAt(OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link Service} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return service builder
		 */
		public Builder updatedAt(Instant updatedAt) {
			return updatedAt(OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Service} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return service builder
		 */
		public Builder updatedAt(OffsetDateTime updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		/**
		 * Creates a new instance of the {@link Service} using the values defined in the builder.
		 *
		 * @return service instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public Service build() {
			Assert.notNull(id, "Service entity identifier can't be null");
			Assert.notNull(namespace, "Namespace entity identifier can't be null");
			Assert.hasText(slug, "Service slug can't be blank");
			Assert.hasText(name, "Service name can't be blank");

			return new Service(id, namespace, slug, name, description, createdAt, updatedAt);
		}

	}
}
