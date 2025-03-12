package com.konfigyr.security;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scopes let you specify exactly what type of access is required for an OAuth client. Scopes limit access for
 * OAuth tokens. They do not grant any additional permission beyond that which the user already has.
 * <p>
 * OAuth scopes can implicitly include other scopes as they are required in order to successfully perform
 * operations that are being granted. For example, in order to delete or update a Namespace, the client
 * needs access to read it first.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public enum OAuthScope implements GrantedAuthority {

	/**
	 * Scope used to indicate that the application intends to use OIDC to verify the user's identity.
	 */
	OPENID(OidcScopes.OPENID),

	/**
	 * Grants read-only access to namespaces.
	 */
	READ_NAMESPACES("namespaces:read"),

	/**
	 * Grants read and write access to namespaces.
	 */
	WRITE_NAMESPACES("namespaces:write", READ_NAMESPACES),

	/**
	 * Grants read, write and delete access to namespaces.
	 */
	DELETE_NAMESPACES("namespaces:delete", WRITE_NAMESPACES),

	/**
	 * Grants read access to namespaces and the option to create and manage invitations.
	 */
	INVITE_MEMBERS("namespaces:invite", READ_NAMESPACES),

	/**
	 * Grants full access to namespace operations like read, write, delete and invite.
	 */
	NAMESPACES("namespaces", READ_NAMESPACES, WRITE_NAMESPACES, DELETE_NAMESPACES, INVITE_MEMBERS);

	private final String value;
	private final Set<OAuthScope> included;

	OAuthScope(String value, OAuthScope... included) {
		this.value = value;
		this.included = aggregate(included);
	}

	/**
	 * Returns the granted authority value of the OAuth scope that would be granted to OAuth2 Access Tokens.
	 *
	 * @return the scope value, never {@literal null}.
	 */
	@NonNull
	@Override
	public String getAuthority() {
		return value;
	}

	/**
	 * Returns a unique collection of {@link OAuthScope OAuth scopes} that are implicitly included when
	 * this scope is used.
	 *
	 * @return included OAuth scopes, never {@literal null}.
	 */
	@NonNull
	public Set<OAuthScope> getIncluded() {
		return included;
	}

	/**
	 * Attempts to resolve the {@link OAuthScope} from its value.
	 *
	 * @param value scope value
	 * @return matching OAuth scope, never {@literal null}
	 * @throws IllegalArgumentException when scope value is blank or unknown.
	 */
	public static OAuthScope from(@Nullable String value) {
		Assert.notNull(value, "OAuth scope can not be blank");

		for (OAuthScope scope : OAuthScope.values()) {
			if (scope.value.equals(value)) {
				return scope;
			}
		}

		throw new InvalidOAuthScopeException(value);
	}

	/**
	 * Parses the {@code scope} parameter from an OAuth2 request and returns a set of {@link OAuthScope OAuth scopes}.
	 *
	 * @param scope requested scopes, can be {@literal null}
	 * @return set of OAuth scopes, never {@literal null}
	 * @throws IllegalArgumentException when there is an unsupported scope
	 */
	public static Set<OAuthScope> parse(@Nullable String scope) {
		final String[] scopes = StringUtils.tokenizeToStringArray(scope, " ");

		if (scopes.length == 0) {
			return Collections.emptySet();
		}

		return Arrays.stream(scopes)
				.map(OAuthScope::from)
				.collect(Collectors.toUnmodifiableSet());
	}

	static Set<OAuthScope> aggregate(OAuthScope... included) {
		final Set<OAuthScope> state = new HashSet<>();

		for (OAuthScope scope : included) {
			state.add(scope);

			if (!CollectionUtils.isEmpty(scope.included)) {
				state.addAll(scope.included);
			}
		}

		return Collections.unmodifiableSet(state);
	}
}
