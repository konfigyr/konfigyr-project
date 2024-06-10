package com.konfigyr.test;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountStatus;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Utility class that contains test {@link Account} stubs.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public interface TestAccounts {

	/**
	 * Creates a {@link Account.Builder} for <code>john.doe@konfigyr.com</code> test account.
	 *
	 * @return John Doe account builder, never {@literal null}
	 */
	static @NonNull Account.Builder john() {
		return Account.builder().id(1L)
				.status(AccountStatus.ACTIVE)
				.email("john.doe@konfigyr.com")
				.firstName("John")
				.lastName("Doe")
				.lastLoginAt(Instant.now().minus(5, ChronoUnit.MINUTES));
	}

	/**
	 * Creates a {@link Account.Builder} for <code>jane.doe@konfigyr.com</code> test account.
	 *
	 * @return Jane Doe account builder, never {@literal null}
	 */
	static @NonNull Account.Builder jane() {
		return Account.builder().id(2L)
				.status(AccountStatus.ACTIVE)
				.email("jane.doe@konfigyr.com")
				.firstName("Jane")
				.lastName("Doe");
	}

}
