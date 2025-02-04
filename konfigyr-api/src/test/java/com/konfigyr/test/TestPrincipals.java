package com.konfigyr.test;

import com.konfigyr.account.Account;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

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
	 * @param account account to be used in the authentication, never {@literal null}
	 * @return Account principal authentication, never {@literal null}
	 */
	static @NonNull Authentication from(@NonNull Account account) {
		return new TestingAuthenticationToken(account.id().serialize(), "", AuthorityUtils.NO_AUTHORITIES);
	}

}
