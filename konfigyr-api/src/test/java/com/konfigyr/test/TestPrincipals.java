package com.konfigyr.test;

import com.konfigyr.account.Account;
import com.konfigyr.security.AuthenticatedPrincipal;
import org.jspecify.annotations.NonNull;
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
	 * Creates an {@link Authentication} for a {@link com.konfigyr.namespace.NamespaceApplication}
	 * that is identified by the given <code>client_id</code>.
	 *
	 * @param id the OAuth 2.0 {@code client_id}, can't be blank
	 * @return OAuth 2.0 Client test authentication, never {@literal null}
	 */
	static @NonNull Authentication application(String id) {
		return from(new TestAuthenticatedPrincipal(id));
	}

	/**
	 * Creates an {@link Authentication} for the given {@link Account}.
	 *
	 * @param account account to be used in the authentication, never {@literal null}
	 * @return Account principal authentication, never {@literal null}
	 */
	static @NonNull Authentication from(@NonNull Account account) {
		return from(new TestAuthenticatedPrincipal(account));
	}

	/**
	 * Creates an {@link Authentication} for the given {@link AuthenticatedPrincipal}.
	 *
	 * @param principal the authenticated principal to be used in the authentication, never {@literal null}
	 * @return authenticated principal authentication, never {@literal null}
	 */
	static @NonNull Authentication from(@NonNull AuthenticatedPrincipal principal) {
		return new TestingAuthenticationToken(principal, "", AuthorityUtils.NO_AUTHORITIES);
	}

}
