package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Slug;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;

/**
 * Record used to define one {@link Service} that would be created via {@link Services service manager}.
 *
 * @param namespace entity identifier of the {@link Namespace} that owns this service, can't be null.
 * @param slug unique service identifier derived from its name, can't be null.
 * @param name a human-friendly name for the service; can't be null.
 * @param description short description for the service; can be null.
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@ValueObject
public record ServiceDefinition(
		@NonNull EntityId namespace,
		@NonNull Slug slug,
		@NonNull String name,
		@Nullable String description
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 7324651962101230810L;

	/**
	 * Creates a new {@link Builder fluent service definition builder} instance used to
	 * create the {@link ServiceDefinition} record.
	 *
	 * @return service definition builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link ServiceDefinition}.
	 */
	public static final class Builder {

		private EntityId namespace;
		private Slug slug;
		private String name;
		private String description;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} of the {@link Namespace} that would be used as a service owner.
		 *
		 * @param namespace internal namespace identifier
		 * @return service definition builder
		 */
		@NonNull
		public Builder namespace(long namespace) {
			return namespace(EntityId.from(namespace));
		}

		/**
		 * Specify the external {@link EntityId} of the {@link Namespace} that would be used as a service owner.
		 *
		 * @param namespace external namespace identifier
		 * @return service definition builder
		 */
		public Builder namespace(String namespace) {
			return namespace(EntityId.from(namespace));
		}

		/**
		 * Specify the {@link EntityId} of the {@link Namespace} that would be used as a service owner.
		 *
		 * @param namespace namespace owner identifier
		 * @return service definition builder
		 */
		public Builder namespace(EntityId namespace) {
			this.namespace = namespace;
			return this;
		}

		/**
		 * Specify the raw slug value that identifies this {@link ServiceDefinition}.
		 *
		 * @param slug service slug value
		 * @return service definition builder
		 * @throws IllegalArgumentException when the slug is invalid
		 */
		public Builder slug(String slug) {
			return slug(Slug.slugify(slug));
		}

		/**
		 * Specify the slug that identifies this {@link ServiceDefinition}.
		 *
		 * @param slug service slug
		 * @return service definition builder
		 */
		public Builder slug(Slug slug) {
			this.slug = slug;
			return this;
		}

		/**
		 * Specify the user-friendly name for this {@link ServiceDefinition}.
		 *
		 * @param name service name
		 * @return service definition builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Describe this {@link ServiceDefinition}.
		 *
		 * @param description service description
		 * @return service definition builder
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Creates a new instance of the {@link ServiceDefinition} using the values defined in the builder.
		 *
		 * @return service definition instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public ServiceDefinition build() {
			Assert.notNull(namespace, "Namespace entity identifier can not be null");
			Assert.notNull(slug, "Namespace slug can not be null");
			Assert.hasText(name, "Namespace name can not be blank");

			return new ServiceDefinition(namespace, slug, name, description);
		}

	}
}
