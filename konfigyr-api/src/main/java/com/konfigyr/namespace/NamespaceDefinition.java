package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.slug.Slug;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;

/**
 * Record used to define one {@link Namespace} that would be created via {@link NamespaceManager}.
 *
 * @param owner entity identifier of the {@link com.konfigyr.account.Account} used as the {@link Namespace} owner
 * @param slug unique namespace identifier derived from it's name, can not be null
 * @param type defines the type of the namespace, can not be null
 * @param name human friendly name of the namespace, can not be null
 * @param description short description of the namespace, can be null
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@ValueObject
public record NamespaceDefinition(
		@NonNull EntityId owner,
		@NonNull Slug slug,
		@NonNull NamespaceType type,
		@NonNull String name,
		@Nullable String description
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 7324651962101230810L;

	/**
	 * Creates a new {@link Builder fluent namespace definition builder} instance used to
	 * create the {@link NamespaceDefinition} record.
	 *
	 * @return namespace definition builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link NamespaceDefinition}.
	 */
	public static final class Builder {

		private EntityId owner;
		private Slug slug;
		private NamespaceType type;
		private String name;
		private String description;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} of the {@link com.konfigyr.account.Account}
		 * that would be set as the owner for the {@link Namespace}.
		 *
		 * @param owner internal namespace owner identifier
		 * @return namespace builder
		 */
		@NonNull
		public Builder owner(Long owner) {
			return owner(EntityId.from(owner));
		}

		/**
		 * Specify the external {@link EntityId} of the {@link com.konfigyr.account.Account}
		 * that would be set as the owner for the {@link Namespace}.
		 *
		 * @param owner external namespace owner identifier
		 * @return namespace builder
		 */
		public Builder owner(String owner) {
			return owner(EntityId.from(owner));
		}

		/**
		 * Specify the {@link EntityId} of the {@link com.konfigyr.account.Account} that would
		 * be set as the owner for the {@link Namespace}.
		 *
		 * @param owner namespace owner identifier
		 * @return namespace builder
		 */
		public Builder owner(EntityId owner) {
			this.owner = owner;
			return this;
		}

		/**
		 * Specify the raw slug value that identifies this {@link NamespaceDefinition}.
		 *
		 * @param slug namespace slug value
		 * @return namespace definition builder
		 * @throws IllegalArgumentException when the slug is invalid
		 */
		public Builder slug(String slug) {
			return slug(Slug.slugify(slug));
		}

		/**
		 * Specify the slug that identifies this {@link NamespaceDefinition}.
		 *
		 * @param slug namespace slug
		 * @return namespace definition builder
		 */
		public Builder slug(Slug slug) {
			this.slug = slug;
			return this;
		}

		/**
		 * Specify the {@link NamespaceType} that should be used by {@link NamespaceDefinition}.
		 *
		 * @param type namespace type
		 * @return namespace definition builder
		 * @throws IllegalArgumentException when type name is invalid
		 */
		public Builder type(String type) {
			return type(NamespaceType.valueOf(type));
		}

		/**
		 * Specify the {@link NamespaceType} that should be used by {@link NamespaceDefinition}.
		 *
		 * @param type namespace type
		 * @return namespace definition builder
		 */
		public Builder type(NamespaceType type) {
			this.type = type;
			return this;
		}

		/**
		 * Specify the user-friendly name for this {@link NamespaceDefinition}.
		 *
		 * @param name namespace name
		 * @return namespace definition builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Describe this {@link NamespaceDefinition}.
		 *
		 * @param description namespace description
		 * @return namespace definition builder
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Creates a new instance of the {@link NamespaceDefinition} using the values defined in the builder.
		 *
		 * @return namespace instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public NamespaceDefinition build() {
			Assert.notNull(owner, "Namespace owner can not be null");
			Assert.notNull(type, "Namespace type can not be null");
			Assert.notNull(slug, "Namespace slug can not be null");
			Assert.hasText(name, "Namespace name can not be blank");

			return new NamespaceDefinition(owner, slug, type, name, description);
		}

	}
}
