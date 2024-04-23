package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.namespace.NamespaceType;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Record defining the user account.
 *
 * @param id unique user account identifier, can not be {@literal null}
 * @param status account status, can not be {@literal null}
 * @param email email address of the user account, can not be {@literal null}
 * @param firstName users first name, can be {@literal null}
 * @param lastName users last name, can be {@literal null}
 * @param displayName users full name or email address, can't be {@literal null}
 * @param avatar URL where the avatar for the user account is hosted, can be {@literal null}
 * @param memberships account namespace memberships, can not be {@literal null}
 * @param lastLoginAt when was the user account last online, can be {@literal null}
 * @param createdAt when was this user account created, can be {@literal null}
 * @param updatedAt when was this user account last updated, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@AggregateRoot
public record Account(
		@NonNull @Identity EntityId id,
		@NonNull AccountStatus status,
		@NonNull String email,
		@Nullable String firstName,
		@Nullable String lastName,
		@NonNull String displayName,
		@Nullable String avatar,
		@NonNull @Association(aggregateType = Account.class) Memberships memberships,
		@Nullable OffsetDateTime lastLoginAt,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 294304163437354662L;

	/**
	 * To successfully delete the {@link Account} user accounts are required to leave or delete
	 * {@link com.konfigyr.namespace.Namespace non-personal namespaces} where they are specified as
	 * {@link NamespaceRole#ADMIN administrors}.
	 * <p>
	 * Keep in mind that any {@link com.konfigyr.namespace.Namespace namespaces} with a
	 * {@link NamespaceType#PERSONAL personal type} would automatically be deleted with the account.
	 */
	public boolean isDeletable() {
		return memberships.stream()
				.noneMatch(membership -> membership.type() != NamespaceType.PERSONAL
						&& membership.role() == NamespaceRole.ADMIN);
	}

	/**
	 * Creates a new {@link Builder fluent account builder} instance used to create
	 * the {@link Account} record.
	 *
	 * @return account builder, never {@literal null}
	 */
	@NonNull
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Creates a new {@link Builder fluent account builder} instance and pre-populate its
	 * state from the given{@link Account} record.
	 *
	 * @param account account data to be copied, can't be {@literal null}
	 * @return account builder, never {@literal null}
	 */
	@NonNull
	public static Builder builder(@NonNull Account account) {
		return new Builder(account);
	}

	/**
	 * Fluent builder type used to create an {@link Account}.
	 */
	public static final class Builder {

		private EntityId id;
		private AccountStatus status;
		private String email;
		private String firstName;
		private String lastName;
		private String avatar;
		private List<Membership> memberships;
		private OffsetDateTime lastLoginAt;
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;

		private Builder() {
		}

		private Builder(@NonNull Account account) {
			id = account.id();
			status = account.status();
			email = account.email();
			firstName = account.firstName();
			lastName = account.lastName();
			avatar = account.avatar();
			lastLoginAt = account.lastLoginAt();
			createdAt = account.createdAt();
			updatedAt = account.updatedAt();

			// copy the memberships using the builder method
			memberships(account.memberships());
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link Account}.
		 *
		 * @param id internal account identifier
		 * @return account builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link Account}.
		 *
		 * @param id external account identifier
		 * @return account builder
		 */
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link Account}.
		 *
		 * @param id account identifier
		 * @return account builder
		 */
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the email address used by this {@link Account}.
		 *
		 * @param email email address
		 * @return account builder
		 */
		public Builder email(String email) {
			this.email = email;
			return this;
		}

		/**
		 * Specify the {@link AccountStatus} of the {@link Account}.
		 *
		 * @param status account status
		 * @return account builder
		 * @throws IllegalArgumentException when status is invalid
		 */
		public Builder status(String status) {
			return status(AccountStatus.valueOf(status));
		}

		/**
		 * Specify the {@link AccountStatus} of the {@link Account}.
		 *
		 * @param status account status
		 * @return account builder
		 */
		public Builder status(AccountStatus status) {
			this.status = status;
			return this;
		}

		/**
		 * Specify the first name of the {@link Account}.
		 *
		 * @param firstName first name
		 * @return account builder
		 */
		public Builder firstName(String firstName) {
			this.firstName = firstName;
			return this;
		}

		/**
		 * Specify the last name of the {@link Account}.
		 *
		 * @param lastName first name
		 * @return account builder
		 */
		public Builder lastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		/**
		 * Specify the location of the {@link Account} avatar or profile image.
		 *
		 * @param avatar profile image location
		 * @return account builder
		 */
		public Builder avatar(String avatar) {
			this.avatar = avatar;
			return this;
		}

		/**
		 * Adds a {@link com.konfigyr.namespace.Namespace} {@link Membership} for this account.
		 *
		 * @param membership membership to be added.
		 * @return account builder
		 */
		public Builder membership(Membership membership) {
			if (membership != null) {
				if (memberships == null) {
					memberships = new ArrayList<>();
				}

				memberships.add(membership);
			}
			return this;
		}

		/**
		 * Adds a {@link com.konfigyr.namespace.Namespace} {@link Membership memberships} for this account.
		 *
		 * @param memberships memberships to be added.
		 * @return account builder
		 */
		public Builder memberships(Iterable<Membership> memberships) {
			if (memberships != null) {
				memberships.forEach(this::membership);
			}
			return this;
		}

		/**
		 * Specify when this {@link Account} was last logged-in.
		 *
		 * @param lastLoginAt last login date
		 * @return account builder
		 */
		public Builder lastLoginAt(Instant lastLoginAt) {
			return lastLoginAt(OffsetDateTime.ofInstant(lastLoginAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Account} was last logged-in.
		 *
		 * @param lastLoginAt last login date
		 * @return account builder
		 */
		public Builder lastLoginAt(OffsetDateTime lastLoginAt) {
			this.lastLoginAt = lastLoginAt;
			return this;
		}

		/**
		 * Specify when this {@link Account} was created.
		 *
		 * @param createdAt created date
		 * @return account builder
		 */
		public Builder createdAt(Instant createdAt) {
			return createdAt(OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Account} was created.
		 *
		 * @param createdAt created date
		 * @return account builder
		 */
		public Builder createdAt(OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link Account} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return account builder
		 */
		public Builder updatedAt(Instant updatedAt) {
			return updatedAt(OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link Account} was last updated.
		 *
		 * @param updatedAt updated date
		 * @return account builder
		 */
		public Builder updatedAt(OffsetDateTime updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		private String generateDisplayName() {
			final StringJoiner joiner = new StringJoiner(" ");

			if (StringUtils.hasText(firstName)) {
				joiner.add(firstName);
			}

			if (StringUtils.hasText(lastName)) {
				joiner.add(lastName);
			}

			if (joiner.length() == 0) {
				joiner.add(email);
			}

			return joiner.toString();
		}

		/**
		 * Creates a new instance of the {@link Account} using the values defined in the builder.
		 *
		 * @return account instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public Account build() {
			Assert.notNull(id, "Account entity identifier can not be null");
			Assert.notNull(status, "Account status can not be null");
			Assert.hasText(email, "Account email address can not be blank");

			final Memberships memberships = CollectionUtils.isEmpty(this.memberships) ?
					Memberships.empty() : Memberships.of(this.memberships);

			return new Account(id, status, email, firstName, lastName, generateDisplayName(),
					avatar, memberships, lastLoginAt, createdAt, updatedAt);
		}

	}

}
