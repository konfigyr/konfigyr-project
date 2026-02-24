package com.konfigyr.security.oauth;

import com.konfigyr.entity.EntityId;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.OAuthScopes;
import com.konfigyr.security.PrincipalType;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * A Spring Security {@link JwtAuthenticationToken} that exposes the Konfigyr {@link AuthenticatedPrincipal}.
 *<p>
 * This authentication token acts as a boundary adapter between Spring Security's JWT infrastructure and
 * Konfigyr's security abstraction model and encapsulates the following:
 * <ul>
 *     <li>The verified {@link Jwt}</li>
 *     <li>The resolved {@link AuthenticatedPrincipal}</li>
 *     <li>The granted authorities derived from {@link OAuthScopes}</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class AuthenticatedPrincipalAuthenticationToken extends JwtAuthenticationToken {

	private final AuthenticatedPrincipal principal;

	private AuthenticatedPrincipalAuthenticationToken(String subject, PrincipalType type, Jwt jwt) {
		this(new OAuthAuthenticatedPrincipal(subject, type, jwt), jwt);
	}

	private AuthenticatedPrincipalAuthenticationToken(OAuthAuthenticatedPrincipal principal, Jwt jwt) {
		super(jwt, authoritiesFor(jwt), principal.get());
		this.principal = principal;
	}

	/**
	 * Returns the Konfigyr authenticated principal.
	 * <p>
	 * This overrides the default behavior of {@link JwtAuthenticationToken} to expose a strongly-typed
	 * {@link AuthenticatedPrincipal} instead of raw JWT claim data.
	 *
	 * @return the authenticated principal, never {@code null}
	 */
	@NonNull
	@Override
	public AuthenticatedPrincipal getPrincipal() {
		return principal;
	}

	/**
	 * Creates a new authenticated principal authentication token from the verified {@link Jwt}.
	 *
	 * @param jwt the verified JWT, must not be {@code null}
	 * @return the authenticated principal authentication token, never {@code null}
	 */
	@NonNull
	public static AuthenticatedPrincipalAuthenticationToken of(@NonNull Jwt jwt) {
		final String subject = jwt.getSubject();

		if (subject == null || subject.isEmpty()) {
			throw new InvalidBearerTokenException("JWT subject claim must not be empty or null");
		}

		final PrincipalType type = principalTypeFor(subject);

		if (type == null) {
			throw new InvalidBearerTokenException("JWT subject claim does not match a valid Konfigyr principal");
		}

		return new AuthenticatedPrincipalAuthenticationToken(subject, type, jwt);
	}

	static PrincipalType principalTypeFor(@NonNull String subject) {
		if (subject.startsWith("kfg-")) {
			return PrincipalType.OAUTH_CLIENT;
		}

		try {
			EntityId.from(subject);
		} catch (IllegalArgumentException ex) {
			return null;
		}

		return PrincipalType.USER_ACCOUNT;
	}

	static Collection<GrantedAuthority> authoritiesFor(Jwt jwt) {
		final Collection<GrantedAuthority> authorities = new HashSet<>(OAuthScopes.from(jwt).toAuthorities());

		final FactorGrantedAuthority.Builder builder = FactorGrantedAuthority
				.withAuthority(FactorGrantedAuthority.BEARER_AUTHORITY);

		if (jwt.getIssuedAt() != null) {
			builder.issuedAt(jwt.getIssuedAt());
		}

		authorities.add(builder.build());
		return Collections.unmodifiableCollection(authorities);
	}
}
