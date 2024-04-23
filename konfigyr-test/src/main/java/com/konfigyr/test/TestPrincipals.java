package com.konfigyr.test;

import com.konfigyr.account.Account;
import com.konfigyr.security.AccountPrincipal;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

/***
 * Utility class that contains test {@link Authentication} stubs.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public interface TestPrincipals {

	/**
	 * Creates an {@link Authentication} for <code>john.doe@konfigyr.com</code> test account.
	 *
	 * @return John Doe authentication, never {@literal null}
	 */
	static @NonNull Authentication john() {
		return from(TestAccounts.john().build());
	}

	/**
	 * Creates an {@link Authentication} for <code>jane.doe@konfigyr.com</code> test account.
	 *
	 * @return Jane Doe authentication, never {@literal null}
	 */
	static @NonNull Authentication jane() {
		return from(TestAccounts.jane().build());
	}

	/**
	 * Creates an {@link Authentication} for the given {@link Account}.
	 *
	 * @param account account to be used as the {@link AccountPrincipal}, never {@literal null}
	 * @return Account authentication, never {@literal null}
	 */
	static @NonNull Authentication from(@NonNull Account account) {
		return from(AccountPrincipal.from(account));
	}

	/**
	 * Creates an {@link Authentication} for the given {@link AccountPrincipal}.
	 *
	 * @param principal account principal, never {@literal null}
	 * @return Account principal authentication, never {@literal null}
	 */
	static @NonNull Authentication from(@NonNull AccountPrincipal principal) {
		return new TestingAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
	}

}
