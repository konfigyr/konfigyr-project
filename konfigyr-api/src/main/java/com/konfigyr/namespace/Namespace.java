package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.AggregateRoot;
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
 * Namespaces provide one single place to organize artifacts and vaults.
 *
 * @param id unique namespace identifier, can not be {@literal null}
 * @param slug unique namespace identifier derived from it's name, can not be {@literal null}
 * @param type defines the type of the namespace, can not be {@literal null}
 * @param name human friendly name of the namespace, can not be {@literal null}
 * @param description short description of the namespace, can be {@literal null}
 * @param createdAt when was this namespace created, can be {@literal null}
 * @param updatedAt when was this namespace last updated, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@AggregateRoot
public record Namespace(
		@NonNull @Identity EntityId id,
		@NonNull String slug,
		@NonNull NamespaceType type,
		@NonNull String name,
		@Nullable String description,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 7324651962101230810L;

	/**
	 * Creates a new {@link Builder fluent namespace builder} instance used to create
	 * the {@link Namespace} record.
	 *
	 * @return namespace builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link Namespace}.
	 */
	public static final class Builder {

		private EntityId id;
		private String slug;
		private NamespaceType type;
		private String name;
		private String description;
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link Namespace}.
		 *
		 * @param id internal namespace identifier
		 * @return namespace builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link Namespace}.
		 *
		 * @param id external namespace identifier
		 * @return namespace builder
		 */
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link Namespace}.
		 *
		 * @param id namespace identifier
		 * @return namespace builder
		 */
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the slug that identifies this {@link Namespace}.
		 *
		 * @param slug namespace slug
		 * @return namespace builder
		 */
		public Builder slug(String slug) {
			this.slug = slug;
			return this;
		}

		/**
		 * Specify the {@link NamespaceType} that should be used by {@link Namespace}.
		 *
		 * @param type namespace type
		 * @return namespace builder
		 * @throws IllegalArgumentException when type name is invalid
		 */
		public Builder type(String type) {
			return type(NamespaceType.valueOf(type));
		}

		/**
		 * Specify the {@link NamespaceType} that should be used by {@link Namespace}.
		 *
		 * @param type namespace type
		 * @return namespace builder
		 */
		public Builder type(NamespaceType type) {
			this.type = type;
			return this;
		}

		/**
		 * Specify the user-friendly name for this {@link Namespace}.
		 *
		 * @param name namespace name
		 * @return namespace builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Describe this {@link Namespace}.
		 *
		 * @param description namespace description
		 * @return namespace builder
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Specify when this {@link Namespace} was created.
		 *
		 * @param createdAt created date
		 * @return namespace builder
		 */
		public Builder createdAt(Instant createdAt) {
			return createdAt(OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Namespace} was created.
		 *
		 * @param createdAt created date
		 * @return namespace builder
		 */
		public Builder createdAt(OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link Namespace} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return namespace builder
		 */
		public Builder updatedAt(Instant updatedAt) {
			return updatedAt(OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Namespace} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return namespace builder
		 */
		public Builder updatedAt(OffsetDateTime updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		/**
		 * Creates a new instance of the {@link Namespace} using the values defined in the builder.
		 *
		 * @return namespace instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public Namespace build() {
			Assert.notNull(id, "Namespace entity identifier can not be null");
			Assert.notNull(type, "Namespace type can not be null");
			Assert.hasText(slug, "Namespace slug can not be blank");
			Assert.hasText(name, "Namespace name can not be blank");

			return new Namespace(id, slug, type, name, description, createdAt, updatedAt);
		}

	}
}
