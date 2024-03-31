package com.konfigyr.account;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * Record that defines which data is needed to create a new {@link Account} using the
 * {@link AccountManager}.
 *
 * @param email email address of the user account, can not be {@literal null}
 * @param firstName users first name, can be {@literal null}
 * @param lastName users last name, can be {@literal null}
 * @param avatar URL where the avatar for the user account is hosted, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
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
		 * @param fullName full name
		 * @return account registration builder
		 */
		public Builder fullName(String fullName) {
			if (!StringUtils.hasText(fullName)) {
				return this;
			}

			final StringTokenizer tokenizer = new StringTokenizer(fullName);

			if (tokenizer.hasMoreTokens()) {
				this.firstName = tokenizer.nextToken();
			}

			final StringBuilder builder = new StringBuilder();
			while (tokenizer.hasMoreTokens()) {
				builder.append(tokenizer.nextToken());

				if (tokenizer.hasMoreTokens()) {
					builder.append(" ");
				}
			}

			if (!builder.isEmpty()) {
				this.lastName = builder.toString();
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
		 * @throws IllegalArgumentException when required is missing or invalid
		 */
		public AccountRegistration build() {
			Assert.hasText(email, "Account email address can not be blank");

			return new AccountRegistration(email, firstName, lastName, avatar);
		}

	}
}
