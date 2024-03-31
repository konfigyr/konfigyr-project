package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * @param lastLoginAt when was the user account last online, can be {@literal null}
 * @param createdAt when was this user account created, can be {@literal null}
 * @param updatedAt when was this user account last updated, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public record Account(
		@NonNull EntityId id,
		@NonNull AccountStatus status,
		@NonNull String email,
		@Nullable String firstName,
		@Nullable String lastName,
		@NonNull String displayName,
		@Nullable String avatar,
		@Nullable OffsetDateTime lastLoginAt,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 294304163437354662L;

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
	 * Fluent builder type used to create an {@link Account}.
	 */
	public static final class Builder {

		private EntityId id;
		private AccountStatus status;
		private String email;
		private String firstName;
		private String lastName;
		private String avatar;
		private OffsetDateTime lastLoginAt;
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;

		private Builder() {
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
		 * @throws IllegalArgumentException when required is missing or invalid
		 */
		@NonNull
		public Account build() {
			Assert.notNull(id, "Account entity identifier can not be null");
			Assert.notNull(status, "Account status can not be null");
			Assert.hasText(email, "Account email address can not be blank");

			return new Account(id, status, email, firstName, lastName, generateDisplayName(),
					avatar, lastLoginAt, createdAt, updatedAt);
		}

	}

}
