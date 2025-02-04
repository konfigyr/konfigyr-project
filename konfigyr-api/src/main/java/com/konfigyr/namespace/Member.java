package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.FullName;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Immutable entity that defines a member of a {@link Namespace}. Members are {@link com.konfigyr.account.Account user accounts}
 * that are given certain rights, via {@link NamespaceRole namespace roles} within a {@link Namespace}.
 * <p>
 * When the {@link Namespace} is created, the {@link com.konfigyr.account.Account} that creates is
 * automatically added as an {@link NamespaceRole#ADMIN administrator} of the namespace. Additional
 * members can be invited to a {@link Namespace} by the administrators if the {@link NamespaceType} supports it.
 * <p>
 * When managing {@link Member namespace members} it is important to highlight that there must be at least
 * one administrator member in the {@link Namespace}, unless it's not of {@link NamespaceType#PERSONAL} type.
 * Personal namespaces should be removed when the administrator account is removed from the namespace or deleted
 * from the application.
 *
 * @param id unique namespace member identifier, can't be {@literal null}
 * @param namespace identifier of the namespace to which this member belongs to, can't be {@literal null}
 * @param account identifier of the account used by the member, can't be {@literal null}
 * @param role role of this member within the namespace, can't be {@literal null}
 * @param email email address of this member, can't be {@literal null}
 * @param fullName full name of this member, can't be {@literal null}
 * @param avatar member profile avatar, can't be {@literal null}
 * @param since when did this member join the namespace, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Entity
public record Member(
		@NonNull @Identity EntityId id,
		@NonNull EntityId namespace,
		@NonNull EntityId account,
		@NonNull NamespaceRole role,
		@NonNull String email,
		@NonNull FullName fullName,
		@NonNull Avatar avatar,
		@Nullable OffsetDateTime since
) implements Serializable {

	@Serial
	private static final long serialVersionUID = -9192528953125322241L;

	/**
	 * Returns the first name for this {@link Member}.
	 *
	 * @return member's first name, can't be {@literal null}
	 */
	@NonNull
	public String firstName() {
		return fullName.firstName();
	}

	/**
	 * Returns the last name for this {@link Member}.
	 *
	 * @return member's last name, can't be {@literal null}
	 */
	@NonNull
	public String lastName() {
		return fullName.lastName();
	}

	/**
	 * Returns the full name as a string to display the full name of the {@link Member}.
	 *
	 * @return full name string representation, can't be {@literal null}
	 */
	@NonNull
	public String displayName() {
		return fullName.get();
	}

	/**
	 * Checks if this {@link Member} is part of the given {@link Namespace}.
	 *
	 * @param namespace namespace to check, can't be {@literal null}
	 * @return {@code true} when this {@link Member} is part of the {@link Namespace}
	 */
	public boolean isMemberOf(@NonNull Namespace namespace) {
		return this.namespace.equals(namespace.id());
	}

	/**
	 * Creates a new {@link Builder fluent namespace member builder} instance used to create
	 * the {@link Member} record.
	 *
	 * @return namespace member builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link Member}.
	 */
	public static final class Builder {

		private EntityId id;
		private EntityId namespace;
		private EntityId account;
		private NamespaceRole role;
		private String email;
		private FullName fullName;
		private Avatar avatar;
		private OffsetDateTime since;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link Member}.
		 *
		 * @param id internal namespace member identifier
		 * @return namespace member builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link Member}.
		 *
		 * @param id external namespace member identifier
		 * @return namespace member builder
		 */
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link Member}.
		 *
		 * @param id namespace member identifier
		 * @return namespace member builder
		 */
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the internal {@link EntityId} of the {@link Namespace} to which
		 * this {@link Member} belongs to.
		 *
		 * @param id internal namespace identifier
		 * @return namespace member builder
		 */
		@NonNull
		public Builder namespace(Long id) {
			return namespace(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} of the {@link Namespace} to which
		 * this {@link Member} belongs to.
		 *
		 * @param id external namespace identifier
		 * @return namespace member builder
		 */
		public Builder namespace(String id) {
			return namespace(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} of the {@link Namespace} to which
		 * this {@link Member} belongs to.
		 *
		 * @param id namespace identifier
		 * @return namespace member builder
		 */
		public Builder namespace(EntityId id) {
			this.namespace = id;
			return this;
		}

		/**
		 * Specify the internal {@link EntityId} of the {@link com.konfigyr.account.Account} behind this {@link Member}.
		 *
		 * @param id internal account identifier
		 * @return namespace member builder
		 */
		@NonNull
		public Builder account(Long id) {
			return account(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} of the {@link com.konfigyr.account.Account} behind this {@link Member}.
		 *
		 * @param id external account identifier
		 * @return namespace member builder
		 */
		public Builder account(String id) {
			return account(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} of the {@link com.konfigyr.account.Account} behind this {@link Member}.
		 *
		 * @param id account identifier
		 * @return namespace member builder
		 */
		public Builder account(EntityId id) {
			this.account = id;
			return this;
		}

		/**
		 * Specify the role used by this {@link Member} within a {@link Namespace}.
		 *
		 * @param role namespace role value
		 * @return namespace member builder
		 * @throws IllegalArgumentException when role is invalid
		 */
		public Builder role(String role) {
			return role(NamespaceRole.valueOf(role));
		}

		/**
		 * Specify the role used by this {@link Member} within a {@link Namespace}.
		 *
		 * @param role namespace role
		 * @return namespace member builder
		 */
		public Builder role(NamespaceRole role) {
			this.role = role;
			return this;
		}

		/**
		 * Specify the email address used by this {@link Member}.
		 *
		 * @param email email address
		 * @return namespace member builder
		 */
		public Builder email(String email) {
			this.email = email;
			return this;
		}

		/**
		 * Specify the full name of this {@link Member}.
		 *
		 * @param fullName members full name
		 * @return namespace member builder
		 */
		public Builder fullName(String fullName) {
			return fullName(FullName.parse(fullName));
		}

		/**
		 * Specify the {@link FullName} of this {@link Member}.
		 *
		 * @param fullName members full name
		 * @return namespace member builder
		 */
		public Builder fullName(FullName fullName) {
			this.fullName = fullName;
			return this;
		}

		/**
		 * Specify the account avatar location of this {@link Member}.
		 *
		 * @param uri avatar image location
		 * @return namespace member builder
		 */
		public Builder avatar(String uri) {
			return StringUtils.hasText(uri) ? avatar(Avatar.parse(uri)) : this;
		}

		/**
		 * Specify the account {@link Avatar} of this {@link Member}.
		 *
		 * @param avatar account avatar
		 * @return namespace member builder
		 */
		public Builder avatar(Avatar avatar) {
			this.avatar = avatar;
			return this;
		}

		/**
		 * Specify the date as an {@link Instant} when did this {@link com.konfigyr.account.Account} became a
		 * {@link Member} of a {@link Namespace}.
		 *
		 * @param since joined date
		 * @return namespace member builder
		 */
		public Builder since(Instant since) {
			return since(OffsetDateTime.ofInstant(since, ZoneOffset.UTC));
		}

		/**
		 * Specify the date when did this {@link com.konfigyr.account.Account} became a {@link Member}
		 * of a {@link Namespace}.
		 *
		 * @param since joined date
		 * @return namespace member builder
		 */
		public Builder since(OffsetDateTime since) {
			this.since = since;
			return this;
		}

		/**
		 * Creates a new instance of the {@link Member} using the values defined in the builder.
		 *
		 * @return namespace member instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public Member build() {
			Assert.notNull(id, "Member entity identifier can not be null");
			Assert.notNull(namespace, "Namespace entity identifier can not be null");
			Assert.notNull(account, "Account entity identifier can not be null");
			Assert.notNull(role, "Namespace role can not be null");
			Assert.hasText(email, "Member email address can not be blank");
			Assert.notNull(fullName, "Member full name can not be null");

			if (avatar == null) {
				avatar = Avatar.generate(account, fullName.initials());
			}

			return new Member(id, namespace, account, role, email, fullName, avatar, since);
		}
	}
}
