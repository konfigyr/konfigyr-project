package com.konfigyr.account.settings;

import com.konfigyr.account.Account;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.lang.NonNull;

/**
 * Mutable data record that contains {@link Account} data that can be updated via
 * account settings page, mainly the first and last name.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Data
class AccountSettingsForm {

	/**
	 * Creates a form instance using the actual {@link Account} information that
	 * is extracted from the database.
	 *
	 * @param account account information used to fill the form, can't be {@literal null}
	 * @return account form data, never {@literal null}
	 */
	@NonNull
	static AccountSettingsForm from(@NonNull Account account) {
		final AccountSettingsForm form = new AccountSettingsForm();
		form.setEmail(account.email());
		form.setFirstName(account.firstName());
		form.setLastName(account.lastName());
		return form;
	}

	/**
	 * Email address of the {@link Account} that is being managed.
	 */
	private @NotEmpty @Email String email;

	/**
	 * First name of the user behind the {@link Account}.
	 */
	private @NotEmpty @Length(min = 2, max = 60) String firstName;

	/**
	 * Last name of the user behind the {@link Account}.
	 */
	private @NotEmpty @Length(min = 2, max = 60) String lastName;

	/**
	 * Applies the changes contained in this form to the target {@link Account}.
	 *
	 * @param account account to where changes are applied, can't be {@literal null}
	 * @return updated account, never {@literal null}
	 */
	@NonNull
	Account apply(@NonNull Account account) {
		return Account.builder(account)
				.firstName(firstName)
				.lastName(lastName)
				.build();
	}
}
