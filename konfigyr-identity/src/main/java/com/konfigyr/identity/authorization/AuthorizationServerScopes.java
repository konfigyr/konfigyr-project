package com.konfigyr.identity.authorization;

import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

/**
 * Registry of {@link OAuthScope OAuth scopes} that are supported and issued by the Konfigyr
 * Authorization Server (Identity Provider).
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see OAuthScope
 * @see OAuthScopes
 */
public final class AuthorizationServerScopes {

	private static final OAuthScopes scopes = OAuthScopes.of(
			OAuthScope.OPENID,
			OAuthScope.NAMESPACES,
			OAuthScope.PUBLISH_ARTIFACTS,
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
		AuthorizationServerScopes.get().to(scopes, OAuthScope::getAuthority);
	}

}
