package com.konfigyr.security;

import org.jspecify.annotations.NullMarked;

import java.util.Collection;

/**
 * Registry of {@link OAuthScope OAuth scopes} that are supported and enforced by the Konfigyr
 * REST API (OAuth2 Resource Server).
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see OAuthScope
 * @see OAuthScopes
 */
@NullMarked
public final class ResourceServerScopes {

	private static final OAuthScopes scopes = OAuthScopes.of(
			OAuthScope.NAMESPACES,
			OAuthScope.PROFILES,
			OAuthScope.MCP
	);

	private ResourceServerScopes() {
		// this is a utility class
	}

	/**
	 * Returns all the supported OAuth Scopes by the Konfigyr Resource Server.
	 *
	 * @return the supported OAuth Scopes, never {@literal null}
	 */
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
