package com.konfigyr.vault;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.support.Slug;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;

/**
 * Record used to define one {@link Profile} that would be created via {@link ProfileManager Vault manager}.
 *
 * @param service entity identifier of the {@link Service} that owns this profile, can't be null.
 * @param policy the profile access policy; can't be null.
 * @param name a unique profile name with one service; can't be null.
 * @param description short description for the profile; can be null.
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@ValueObject
public record ProfileDefinition(
		@NonNull EntityId service,
		@NonNull ProfilePolicy policy,
		@NonNull Slug slug,
		@NonNull String name,
		@Nullable String description,
		int position
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 6937014235666355801L;

	/**
	 * Creates a new {@link Builder fluent service definition builder} instance used to
	 * create the {@link ProfileDefinition} record.
	 *
	 * @return profile definition builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link ProfileDefinition}.
	 */
	public static final class Builder {

		private EntityId service;
		private ProfilePolicy policy;
		private Slug slug;
		private String name;
		private String description;
		private int position = 1;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} of the {@link Service} that would be used as a profile owner.
		 *
		 * @param service internal profile identifier
		 * @return profile definition builder
		 */
		@NonNull
		public Builder service(long service) {
			return service(EntityId.from(service));
		}

		/**
		 * Specify the external {@link EntityId} of the {@link Service} that would be used as a profile owner.
		 *
		 * @param service external profile identifier
		 * @return profile definition builder
		 */
		public Builder service(String service) {
			return service(EntityId.from(service));
		}

		/**
		 * Specify the {@link EntityId} of the {@link Service} that would be used as a profile owner.
		 *
		 * @param service profile owner identifier
		 * @return profile definition builder
		 */
		public Builder service(EntityId service) {
			this.service = service;
			return this;
		}

		/**
		 * Specify the raw slug value that identifies this {@link ProfileDefinition}.
		 *
		 * @param slug profile slug value
		 * @return profile definition builder
		 * @throws IllegalArgumentException when the slug is invalid
		 */
		public Builder slug(String slug) {
			return slug(Slug.slugify(slug));
		}

		/**
		 * Specify the slug that identifies this {@link Profile}.
		 *
		 * @param slug profile slug value
		 * @return profile definition builder
		 */
		public Builder slug(Slug slug) {
			this.slug = slug;
			return this;
		}

		/**
		 * Specify a unique name for this {@link ProfileDefinition}.
		 *
		 * @param name profile name
		 * @return profile definition builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Describe this {@link ProfileDefinition}.
		 *
		 * @param description profile description
		 * @return profile definition builder
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Defines the profile access policy for this {@link ProfileDefinition}.
		 *
		 * @param policy the profile policy flag
		 * @return profile definition builder
		 */
		public Builder policy(ProfilePolicy policy) {
			this.policy = policy;
			return this;
		}

		/**
		 * Defines the position of the profile in the list of profiles.
		 *
		 * @param position the profile position
		 * @return profile definition builder
		 */
		public Builder position(int position) {
			this.position = position;
			return this;
		}

		/**
		 * Creates a new instance of the {@link ProfileDefinition} using the values defined in the builder.
		 *
		 * @return profile definition instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public ProfileDefinition build() {
			Assert.notNull(service, "Service entity identifier can not be null");
			Assert.notNull(policy, "Profile policy can not be null");
			Assert.notNull(slug, "Profile slug can not be null");
			Assert.isTrue(position > 0, "Profile position must be greater than zero");

			if (name == null) {
				name = slug.get();
			}

			return new ProfileDefinition(service, policy, slug, name, description, position);
		}

	}
	
}
