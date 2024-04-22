package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.namespace.NamespaceType;
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
import java.util.Objects;

/**
 * Immutable entity that defines a membership of an {@link Account} to a {@link Namespace}.
 *
 * @param id unique membership identifier, can't be {@literal null}
 * @param namespace namespace slug to which this account is a member of, can't be {@literal null}
 * @param type type of the namespace, can't be {@literal null}
 * @param role role of this account within the namespace, can't be {@literal null}
 * @param name namespace name to which this account is a member of, can't be {@literal null}
 * @param avatar URL where the avatar for the namespace is hosted, can be {@literal null}
 * @param since when did this account join the namespace, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see com.konfigyr.namespace.Member
 **/
@Entity
public record Membership(
		@NonNull @Identity EntityId id,
		@NonNull @Association(aggregateType = Namespace.class) String namespace,
		@NonNull NamespaceType type,
		@NonNull NamespaceRole role,
		@NonNull String name,
		@Nullable String avatar,
		@Nullable OffsetDateTime since
) implements Comparable<Membership>, Serializable {

	@Serial
	private static final long serialVersionUID = 7685016252518974775L;

	@Override
	public int compareTo(@NonNull Membership membership) {
		if (since == null) {
			return membership.since == null ? 0 : 1;
		}
		if (membership.since == null) {
			return -1;
		}
		return Objects.compare(since, membership.since, OffsetDateTime::compareTo);
	}

	/**
	 * Creates a new {@link Builder fluent membership builder} instance used to create
	 * the {@link Membership} record.
	 *
	 * @return membership builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link Membership}.
	 */
	public static final class Builder {

		private EntityId id;
		private String namespace;
		private NamespaceType type;
		private NamespaceRole role;
		private String name;
		private String avatar;
		private OffsetDateTime since;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link Membership}.
		 *
		 * @param id internal namespace member identifier
		 * @return membership builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link Membership}.
		 *
		 * @param id external namespace member identifier
		 * @return membership builder
		 */
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link Membership}.
		 *
		 * @param id namespace member identifier
		 * @return membership builder
		 */
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the {@link Namespace} URL slug to which this {@link Membership} belongs to.
		 *
		 * @param namespace namespace slug
		 * @return membership builder
		 */
		@NonNull
		public Builder namespace(String namespace) {
			this.namespace = namespace;
			return this;
		}

		/**
		 * Specify the {@link NamespaceType type} of the {@link Namespace} that defines this {@link Membership}.
		 *
		 * @param type namespace type value
		 * @return membership builder
		 * @throws IllegalArgumentException when type is invalid
		 */
		public Builder type(String type) {
			return type(NamespaceType.valueOf(type));
		}

		/**
		 * Specify the {@link NamespaceType type} of the {@link Namespace} that defines this {@link Membership}.
		 *
		 * @param type namespace type
		 * @return membership builder
		 */
		public Builder type(NamespaceType type) {
			this.type = type;
			return this;
		}

		/**
		 * Specify the role used by this {@link Membership} within a {@link Namespace}.
		 *
		 * @param role namespace role value
		 * @return membership builder
		 * @throws IllegalArgumentException when role is invalid
		 */
		public Builder role(String role) {
			return role(NamespaceRole.valueOf(role));
		}

		/**
		 * Specify the role used by this {@link Membership} within a {@link Namespace}.
		 *
		 * @param role namespace role
		 * @return membership builder
		 */
		public Builder role(NamespaceRole role) {
			this.role = role;
			return this;
		}

		/**
		 * Specify the display name of the {@link Namespace}.
		 *
		 * @param name email address
		 * @return membership builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Specify the avatar URL name of the {@link Namespace}.
		 *
		 * @param avatar profile image location
		 * @return membership builder
		 */
		public Builder avatar(String avatar) {
			this.avatar = avatar;
			return this;
		}

		/**
		 * Specify the date, as an {@link Instant}, when was this {@link Membership} created.
		 *
		 * @param since joined date
		 * @return membership builder
		 */
		public Builder since(Instant since) {
			return since(OffsetDateTime.ofInstant(since, ZoneOffset.UTC));
		}

		/**
		 * Specify the date when was this {@link Membership} created.
		 *
		 * @param since joined date
		 * @return membership builder
		 */
		public Builder since(OffsetDateTime since) {
			this.since = since;
			return this;
		}

		/**
		 * Creates a new instance of the {@link Membership} using the values defined in the builder.
		 *
		 * @return membership instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public Membership build() {
			Assert.notNull(id, "Member entity identifier can not be null");
			Assert.notNull(type, "Namespace type can not be null");
			Assert.notNull(role, "Namespace role can not be null");
			Assert.hasText(namespace, "Namespace slug can not be blank");
			Assert.hasText(name, "Namespace name can not be blank");

			return new Membership(id, namespace, type, role, name, avatar, since);
		}
	}
}
