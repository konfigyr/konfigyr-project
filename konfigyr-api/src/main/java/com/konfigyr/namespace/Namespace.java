package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Avatar;
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

/**
 * Namespaces provide one single place to organize artifacts and vaults.
 *
 * @param id unique namespace identifier, can not be {@literal null}
 * @param slug unique namespace identifier derived from its name, can not be {@literal null}
 * @param name human friendly name of the namespace, can not be {@literal null}
 * @param description short description of the namespace, can be {@literal null}
 * @param avatar namespace avatar image resource, can be {@literal null}
 * @param createdAt when was this namespace created, can be {@literal null}
 * @param updatedAt when was this namespace last updated, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@AggregateRoot
public record Namespace(
		@NonNull @Identity EntityId id,
		@NonNull String slug,
		@NonNull String name,
		@Nullable String description,
		@NonNull Avatar avatar,
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
		private String name;
		private String description;
		private Avatar avatar;
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
		 * Specify the location of the {@link Namespace} avatar.
		 *
		 * @param uri namespace avatar image location
		 * @return namespace builder
		 */
		public Builder avatar(String uri) {
			return StringUtils.hasText(uri) ? avatar(Avatar.parse(uri)) : this;
		}

		/**
		 * Specify the {@link Avatar} for the {@link Namespace}.
		 *
		 * @param avatar namespace avatar
		 * @return namespace builder
		 */
		public Builder avatar(Avatar avatar) {
			this.avatar = avatar;
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
			Assert.hasText(slug, "Namespace slug can not be blank");
			Assert.hasText(name, "Namespace name can not be blank");

			if (avatar == null) {
				avatar = Avatar.generate(slug, name.substring(0, 1));
			}

			return new Namespace(id, slug, name, description, avatar, createdAt, updatedAt);
		}

	}
}
