package com.konfigyr.security;

import org.jspecify.annotations.NonNull;

import java.util.Collection;

public final class ResourceServerScopes {

	private static final OAuthScopes scopes = OAuthScopes.of(
			OAuthScope.NAMESPACES,
			OAuthScope.PROFILES
	);

	private ResourceServerScopes() {
		// this is a utility class
	}

	/**
	 * Returns all the supported OAuth Scopes by the Konfigyr Resource Server.
	 *
	 * @return the supported OAuth Scopes, never {@literal null}
	 */
	@NonNull
	public static OAuthScopes get() {
		return scopes;
	}

	/**
	 * Method that would register the Konfigyr Resource Server OAuth scopes by adding them to
	 * the given collection of scopes.
	 *
	 * @param scopes collection of scopes to be customized, never {@literal null}
	 */
	public static void register(Collection<String> scopes) {
		ResourceServerScopes.get().to(scopes, OAuthScope::getAuthority);
	}

}
