package com.konfigyr.vault;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.support.Slug;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Profiles represent logical configuration environments for a {@link com.konfigyr.namespace.Service}.
 * <p>
 * Conceptually, a {@link Profile} models an operational context such as {@code development}, {@code staging},
 * or {@code production}. It is the authoritative source of configuration values that are ultimately served
 * to applications at runtime.
 * <p>
 * Users can choose which {@link ProfilePolicy} their {@link Profile}s should use depending on their internal
 * process and security policies. Protected profiles require that {@code ChangeSet}s must go through a review
 * and an approval process, represented by the {@code ChangeRequest}, before changes can be applied.
 * Unprotected {@link Profile}s allow users to apply their {@code ChangeSet}s directly without going
 * through a review process.
 * <p>
 * When working with {@link Profile}s, it is important to understand the following principles:
 * <ul>
 *     <li>
 *         A {@link Profile} has exactly one authoritative configuration state at any time. This state
 *         is being consumed by applications at runtime.
 *     </li>
 *     <li>
 *         Profiles themselves are not versioned or mutated directly. All changes made to the {@link Profile}
 *         occur indirectly via {@code ChangeSet}s, or {@code ChangeRequest}s if a profile is protected.
 *     </li>
 *     <li>
 *         Historical evolution of a {@link Profile} can be derived from applied {@code ChangeSet}s
 *         and recorded as ChangeHistory entries.
 *     </li>
 * </ul>
 * <p>
 * From a UI and UX perspective, profiles are what users select when choosing <strong>where</strong> they
 * are working, but they should <strong>never</strong> be allowed to edit the profile directly. When selecting
 * a profile, a user should be able to see:
 * <ul>
 *     <li>
 *         The currently active {@code ChangeSet} or the current authoritative state of the {@link Profile}
 *         which they can edit by creating a new draft {@code ChangeSet} as soon as a change is made.
 *     </li>
 *     <li>
 *         Validation rules and metadata for each configuration property that is present in the
 *         active {@code ChangeSet} or the current authoritative state of the {@link Profile}.
 *     </li>
 *     <li>
 *         Approval requirements and how their changes can be applied to the {@link Profile}.
 *     </li>
 * </ul>
 * <p>
 * Profiles can be regarded as a governance boundary, not a version control abstraction. Users should
 * never be exposed to branches, commits, or merges.
 *
 * @param id unique entity identifier of the profile, can't be {@literal null}.
 * @param service unique identifier of the {@link Service} this profile belongs to, can't be {@literal null}.
 * @param slug unique profile identifier derived from its name, can't be {@literal null}.
 * @param name human-readable profile name, can't be {@literal null}.
 * @param description short description of the profile, can be {@literal null}.
 * @param policy access policy for this profile, can't be {@literal null}.
 * @param position the display order of the profile within the UI. Can't be negative or zero.
 * @param createdAt when was this profile created, can be {@literal null}.
 * @param updatedAt when was this profile last updated, can be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ProfilePolicy
 */
@Entity
public record Profile(
		@Identity EntityId id,
		@Association(aggregateType = Service.class) EntityId service,
		String slug,
		String name,
		ProfilePolicy policy,
		@Nullable String description,
		int position,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt
) implements Comparable<Profile>, Serializable {

	@Serial
	private static final long serialVersionUID = 2657983964397225835L;

	/**
	 * Creates a new {@link Builder fluent profile builder} instance used to create the {@link Profile}.
	 *
	 * @return profile builder, never {@literal null}
	 */
	@NonNull
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public int compareTo(@NonNull Profile o) {
		return Integer.compare(position, o.position);
	}

	/**
	 * Fluent builder type used to create a {@link Profile}.
	 */
	public static final class Builder {

		private EntityId id;
		private EntityId service;
		private String slug;
		private String name;
		private ProfilePolicy policy;
		private String description;
		private int position = 1;
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;

		private Builder() {
		}

		/**
		 * Specify the {@link EntityId} of this {@link Profile}.
		 *
		 * @param id profile entity identifier, can't be {@literal null}
		 * @return profile builder
		 */
		@NonNull
		public Builder id(@NonNull EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the internal {@link EntityId} of this {@link Profile}.
		 *
		 * @param id internal profile entity identifier
		 * @return profile builder
		 */
		@NonNull
		public Builder id(long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} of the {@link Service} this {@link Profile} belongs to.
		 *
		 * @param service service entity identifier, can't be {@literal null}
		 * @return profile builder
		 */
		@NonNull
		public Builder service(@NonNull EntityId service) {
			this.service = service;
			return this;
		}

		/**
		 * Specify the internal {@link EntityId} of the {@link Service} this {@link Profile} belongs to.
		 *
		 * @param service internal service entity identifier
		 * @return profile builder
		 */
		@NonNull
		public Builder service(long service) {
			return service(EntityId.from(service));
		}

		/**
		 * Specify the slug that uniquely identifies this {@link Profile} within its {@link Service}.
		 *
		 * @param slug profile slug, can't be {@literal null}
		 * @return profile builder
		 */
		@NonNull
		public Builder slug(@NonNull String slug) {
			this.slug = slug;
			return this;
		}

		/**
		 * Specify the human-readable name of this {@link Profile}.
		 *
		 * @param name profile name, can't be {@literal null}
		 * @return profile builder
		 */
		@NonNull
		public Builder name(@NonNull String name) {
			this.name = name;
			return this;
		}

		/**
		 * Specify the {@link ProfilePolicy} of this {@link Profile}.
		 *
		 * @param policy profile access policy, can't be {@literal null}
		 * @return profile builder
		 */
		@NonNull
		public Builder policy(@NonNull ProfilePolicy policy) {
			this.policy = policy;
			return this;
		}

		/**
		 * Describe this {@link Profile}.
		 *
		 * @param description profile description, can be {@literal null}
		 * @return profile builder
		 */
		@NonNull
		public Builder description(@Nullable String description) {
			this.description = description;
			return this;
		}

		/**
		 * Specify the display order of this {@link Profile} within the UI.
		 *
		 * @param position display position, must be greater than zero
		 * @return profile builder
		 */
		@NonNull
		public Builder position(int position) {
			this.position = position;
			return this;
		}

		/**
		 * Specify when this {@link Profile} was created.
		 *
		 * @param createdAt created date, can be {@literal null}
		 * @return profile builder
		 */
		@NonNull
		public Builder createdAt(@Nullable OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link Profile} was last updated.
		 *
		 * @param updatedAt updated date, can be {@literal null}
		 * @return profile builder
		 */
		@NonNull
		public Builder updatedAt(@Nullable OffsetDateTime updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		/**
		 * Creates a new instance of the {@link Profile} using the values defined in the builder.
		 *
		 * @return profile instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public Profile build() {
			Assert.notNull(id, "Profile entity identifier can not be null");
			Assert.notNull(service, "Profile service identifier can not be null");
			Assert.hasText(name, "Profile name can not be blank");
			Assert.notNull(policy, "Profile policy can not be null");
			Assert.isTrue(position > 0, "Profile position must be greater than zero");

			if (slug == null) {
				slug = Slug.slugify(name).get();
			}

			return new Profile(id, service, slug, name, policy, description, position, createdAt, updatedAt);
		}

	}

}
