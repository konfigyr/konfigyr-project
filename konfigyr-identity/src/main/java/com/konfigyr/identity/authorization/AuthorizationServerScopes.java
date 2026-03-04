package com.konfigyr.identity.authorization;

import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

public final class AuthorizationServerScopes {

	private static final OAuthScopes scopes = OAuthScopes.of(
			OAuthScope.OPENID,
			OAuthScope.NAMESPACES,
			OAuthScope.PROFILES
	);

	private AuthorizationServerScopes() {
		// this is a utility class
	}

	/**
	 * Returns all the supported OAuth Scopes by the Konfigyr Identity Server.
	 *
	 * @return the supported OAuth Scopes, never {@literal null}
	 */
	@NonNull
	public static OAuthScopes get() {
		return scopes;
	}

	/**
	 * Method that would register the Konfigyr Identity Server OAuth scopes by adding them to
	 * the given collection of scopes.
	 *
	 * @param scopes collection of scopes to be customized, never {@literal null}
	 */
	public static void register(Collection<String> scopes) {
		AuthorizationServerScopes.get().forEach(scope -> {
			if (scopes.contains(scope.getAuthority())) {
				return;
			}

			scopes.add(scope.getAuthority());

			scope.getIncluded().forEach(included -> scopes.add(included.getAuthority()));
		});
	}

}
