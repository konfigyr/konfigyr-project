package com.konfigyr.identity.authorization.controller;

import com.konfigyr.security.OAuthScope;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * Represents the state of the OAuth 2.0 Scope that should be present when {@link OAuth2AuthorizationConsent}
 * is requested.
 *
 * @param scope scope scope
 * @param authorized is the scope already authorized
 * @author Vladimir Spasic
 * @since 1.0.0
 */
record AuthorizedScope(OAuthScope scope, boolean authorized) implements Serializable {

	@Serial
	private static final long serialVersionUID = 105136646982783787L;

	/**
	 * The actual {@link OAuthScope} value that is part of the OAuth request
	 *.
	 * @return scope value, never {@literal null}
	 */
	@NonNull
	public String value() {
		return scope.getAuthority();
	}

	/**
	 * Generates the message key that would be used by the {@link org.springframework.context.MessageSource}
	 * to load description for this {@link OAuthScope}.
	 *
	 * @return scope description message key, never {@literal null}
	 */
	@NonNull
	public String messageKey() {
		return "konfigyr.oauth.scope." + scope.name();
	}

	/**
	 * Creates a new {@link AuthorizedScope} that was already granted by previous {@link OAuth2AuthorizationConsent}.
	 *
	 * @param value scope scope
	 * @return the authorized scope
	 */
	static AuthorizedScope authorized(OAuthScope value) {
		return new AuthorizedScope(value, true);
	}

	/**
	 * Creates a new {@link AuthorizedScope} that was not yet granted by any {@link OAuth2AuthorizationConsent}.
	 *
	 * @param value scope scope
	 * @return the unauthorized scope
	 */
	static AuthorizedScope unauthorized(OAuthScope value) {
		return new AuthorizedScope(value, false);
	}

	/**
	 * Creates a new set of {@link AuthorizedScope authorized scopes} that should be present in the
	 * page where {@link OAuth2AuthorizationConsent} should be granted or revoked.
	 *
	 * @param requested requested scopes as a single string
	 * @param consent previous consent, if any
	 * @return set of authorized scope states
	 */
	static Set<AuthorizedScope> from(@NonNull String requested, @Nullable OAuth2AuthorizationConsent consent) {
		return from(OAuthScope.parse(requested), consent);
	}

	/**
	 * Creates a new set of {@link AuthorizedScope authorized scopes} that should be present in the
	 * page where {@link OAuth2AuthorizationConsent} should be granted or revoked.
	 *
	 * @param requested requested scopes as an array
	 * @param consent previous consent, if any
	 * @return set of authorized scope states
	 */
	static Set<AuthorizedScope> from(@NonNull Set<OAuthScope> requested, @Nullable OAuth2AuthorizationConsent consent) {
		final Set<String> authorizedScopes = consent == null ? Collections.emptySet() : consent.getScopes();
		final Set<AuthorizedScope> scopes = new LinkedHashSet<>();

		for (final OAuthScope candidate : requested) {
			if (authorizedScopes.contains(candidate.getAuthority())) {
				scopes.add(AuthorizedScope.authorized(candidate));
			} else if (OAuthScope.OPENID != candidate) {
				// openid scope does not require consent
				scopes.add(AuthorizedScope.unauthorized(candidate));
			}
		}

		return Collections.unmodifiableSet(scopes);
	}

}
