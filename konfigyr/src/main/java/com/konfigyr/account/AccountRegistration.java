package com.konfigyr.account;

import com.konfigyr.support.FullName;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;

/**
 * Record that defines which data is needed to create a new {@link Account} using the
 * {@link AccountManager}.
 *
 * @param email email address of the user account, can not be {@literal null}
 * @param firstName users first name, can't be {@literal null}
 * @param lastName users last name, can't be {@literal null}
 * @param avatar URL where the avatar for the user account is hosted, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@ValueObject
public record AccountRegistration(
		String email,
		String firstName,
		String lastName,
		String avatar
) implements Serializable {

	@Serial
	private static final long serialVersionUID = -6602398138052436386L;

	/**
	 * Creates a new {@link Builder fluent account registration builder} instance used to create
	 * the {@link AccountRegistration} record.
	 *
	 * @return account registration builder, never {@literal null}
	 */
	@NonNull
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link AccountRegistration}.
	 */
	public static final class Builder {

		private String email;
		private String firstName;
		private String lastName;
		private String avatar;

		private Builder() {
		}

		/**
		 * Specify the email address that would be used by the {@link Account} that should be registered.
		 *
		 * @param email email address
		 * @return account registration builder
		 */
		public Builder email(String email) {
			this.email = email;
			return this;
		}

		/**
		 * Specify the full name that would be used by the {@link Account} that should be registered.
		 * <p>
		 * This method would try its parse the full name into first and last name parts.
		 *
		 * @param value full name
		 * @return account registration builder
		 * @see FullName
		 */
		public Builder fullName(String value) {
			final FullName fullName = FullName.parse(value);

			if (fullName != null) {
				this.firstName = fullName.firstName();
				this.lastName = fullName.lastName();
			}

			return this;
		}

		/**
		 * Specify the first name that would be used by the {@link Account} that should be registered.
		 *
		 * @param firstName first name
		 * @return account registration builder
		 */
		public Builder firstName(String firstName) {
			this.firstName = firstName;
			return this;
		}

		/**
		 * Specify the last name that would be used by the {@link Account} that should be registered.
		 *
		 * @param lastName last name
		 * @return account registration builder
		 */
		public Builder lastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		/**
		 * Specify the location of the profile image that would be used by the {@link Account} that
		 * should be registered.
		 *
		 * @param avatar profile image location
		 * @return account registration builder
		 */
		public Builder avatar(String avatar) {
			this.avatar = avatar;
			return this;
		}

		/**
		 * Creates a new instance of the {@link AccountRegistration} using the values defined in the builder.
		 *
		 * @return account registration instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		public AccountRegistration build() {
			Assert.hasText(email, "Account email address can not be blank");
			Assert.hasText(firstName, "Account first name can not be blank");
			Assert.hasText(lastName, "Account last name can not be blank");

			return new AccountRegistration(email, firstName, lastName, avatar);
		}

	}
}
