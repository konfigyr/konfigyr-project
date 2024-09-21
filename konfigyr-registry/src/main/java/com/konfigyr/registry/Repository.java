package com.konfigyr.registry;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Entity;
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
 * A Konfigyr repository is a virtual storage that can be created under a {@link Namespace}.
 * <p>
 * It allows storing artifacts, and their versions, or your applications.
 *
 * @param id unique repository identifier, can not be {@literal null}
 * @param namespace namespace slug to which this repository belongs to, can't be {@literal null}
 * @param slug unique repository identifier derived from it's name, can not be {@literal null}
 * @param name human friendly name of the repository, can not be {@literal null}
 * @param description short description of the repository, can be {@literal null}
 * @param isPrivate {@code true} if this repository is considered private (accessible to members only)
 * @param createdAt when was this repository created, can be {@literal null}
 * @param updatedAt when was this repository last updated, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Entity
public record Repository(
		@NonNull @Identity EntityId id,
		@NonNull @Association(aggregateType = Namespace.class) String namespace,
		@NonNull String slug,
		@NonNull String name,
		@Nullable String description,
		boolean isPrivate,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = -3960259620575487442L;

	/**
	 * Creates a new {@link Builder fluent namespace builder} instance used to create
	 * the {@link Repository} record.
	 *
	 * @return repository builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link Repository}.
	 */
	public static final class Builder {
		private EntityId id;
		private String namespace;
		private String slug;
		private String name;
		private String description;
		private boolean isPrivate;
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link Repository}.
		 *
		 * @param id internal repository identifier
		 * @return repository builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link Repository}.
		 *
		 * @param id external repository identifier
		 * @return repository builder
		 */
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link Repository}.
		 *
		 * @param id repository identifier
		 * @return repository builder
		 */
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the {@link Namespace} URL slug to which this {@link Repository} belongs to.
		 *
		 * @param namespace namespace slug
		 * @return repository builder
		 */
		@NonNull
		public Builder namespace(String namespace) {
			this.namespace = namespace;
			return this;
		}

		/**
		 * Specify the slug that identifies this {@link Repository}.
		 *
		 * @param slug repository slug
		 * @return repository builder
		 */
		public Builder slug(String slug) {
			this.slug = slug;
			return this;
		}

		/**
		 * Specify the user-friendly name for this {@link Repository}.
		 *
		 * @param name repository name
		 * @return repository builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Describe this {@link Repository}.
		 *
		 * @param description repository description
		 * @return repository builder
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Specify if this {@link Repository} is private or public.
		 *
		 * @param isPrivate repository visibility
		 * @return repository builder
		 */
		public Builder isPrivate(boolean isPrivate) {
			this.isPrivate = isPrivate;
			return this;
		}

		/**
		 * Specify when this {@link Repository} was created.
		 *
		 * @param createdAt created date
		 * @return repository builder
		 */
		public Builder createdAt(Instant createdAt) {
			return createdAt(OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Repository} was created.
		 *
		 * @param createdAt created date
		 * @return repository builder
		 */
		public Builder createdAt(OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link Repository} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return repository builder
		 */
		public Builder updatedAt(Instant updatedAt) {
			return updatedAt(OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Repository} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return repository builder
		 */
		public Builder updatedAt(OffsetDateTime updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		/**
		 * Creates a new instance of the {@link Repository} using the values defined in the builder.
		 *
		 * @return repository instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public Repository build() {
			Assert.notNull(id, "Repository entity identifier can not be null");
			Assert.hasText(namespace, "Namespace slug can not be blank");
			Assert.hasText(slug, "Repository slug can not be blank");
			Assert.hasText(name, "Repository name can not be blank");

			return new Repository(id, namespace, slug, name, description, isPrivate, createdAt, updatedAt);
		}
	}
}
