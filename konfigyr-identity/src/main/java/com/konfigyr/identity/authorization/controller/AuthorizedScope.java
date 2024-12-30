package com.konfigyr.identity.authorization.controller;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * Represents the state of the OAuth 2.0 Scope that should be present when {@link OAuth2AuthorizationConsent}
 * is requested.
 *
 * @param value scope value
 * @param authorized is the scope already authorized
 * @author Vladimir Spasic
 * @since 1.0.0
 */
record AuthorizedScope(String value, boolean authorized) implements Serializable {

	@Serial
	private static final long serialVersionUID = 105136646982783787L;

	/**
	 * Creates a new {@link AuthorizedScope} that was already granted by previous {@link OAuth2AuthorizationConsent}.
	 *
	 * @param value scope value
	 * @return the authorized scope
	 */
	static AuthorizedScope authorized(String value) {
		return new AuthorizedScope(value, true);
	}

	/**
	 * Creates a new {@link AuthorizedScope} that was not yet granted by any {@link OAuth2AuthorizationConsent}.
	 *
	 * @param value scope value
	 * @return the unauthorized scope
	 */
	static AuthorizedScope unauthorized(String value) {
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
		return from(StringUtils.tokenizeToStringArray(requested, " "), consent);
	}

	/**
	 * Creates a new set of {@link AuthorizedScope authorized scopes} that should be present in the
	 * page where {@link OAuth2AuthorizationConsent} should be granted or revoked.
	 *
	 * @param requested requested scopes as an array
	 * @param consent previous consent, if any
	 * @return set of authorized scope states
	 */
	static Set<AuthorizedScope> from(@NonNull String[] requested, @Nullable OAuth2AuthorizationConsent consent) {
		final Set<String> authorizedScopes = consent == null ? Collections.emptySet() : consent.getScopes();
		final Set<AuthorizedScope> scopes = new LinkedHashSet<>();

		for (final String candidate : requested) {
			if (authorizedScopes.contains(candidate)) {
				scopes.add(AuthorizedScope.authorized(candidate));
			} else if (!candidate.equals(OidcScopes.OPENID)) {
				// openid scope does not require consent
				scopes.add(AuthorizedScope.unauthorized(candidate));
			}
		}

		return Collections.unmodifiableSet(scopes);
	}

}
