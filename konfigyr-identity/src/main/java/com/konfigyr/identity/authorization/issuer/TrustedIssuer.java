package com.konfigyr.identity.authorization.issuer;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Represents a trusted external OIDC identity provider and serves as the verification
 * entry point for JWT subject tokens during an OAuth 2.0 Token Exchange (RFC 8693).
 * <p>
 * In the workload identity token exchange flow a client presents a JWT issued by an
 * external provider (such as GitHub Actions or GitLab) as the {@code subject_token}.
 * This class validates that token, verifying its signature against the issuer's public
 * keys, asserting the {@code iss} and timestamp claims, and optionally enforcing
 * {@code aud} constraints, before the authorization server issues a scoped Konfigyr
 * access token in exchange.
 * <p>
 * A {@link TrustedIssuer} combines the static configuration in a
 * {@link TrustedIssuerRegistration} with a pre-configured verifier backed by the
 * issuer's live JWK source. Instances are produced by {@link TrustedIssuerRegistry}
 * and are not constructed directly. Equality and hash code are based solely on the
 * underlying {@link TrustedIssuerRegistration}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see TrustedIssuerRegistry
 * @see TrustedIssuerRegistration
 */
@NullMarked
@RequiredArgsConstructor
@EqualsAndHashCode(of = "registration")
public final class TrustedIssuer {

	private final TrustedIssuerRegistration registration;
	private final JwtDecoder delegate;

	/**
	 * Returns the stored registration data for this issuer.
	 *
	 * @return the {@link TrustedIssuerRegistration}, never {@code null}
	 */
	public TrustedIssuerRegistration registration() {
		return registration;
	}

	/**
	 * Decodes and validates the given compact JWT string, applying the issuer timestamp
	 * and audience constraints configured in the {@link TrustedIssuerRegistration}.
	 *
	 * @param token the compact JWT string to verify
	 * @return the validated JWT claims
	 * @throws AuthenticationException if the token is malformed, expired, or fails
	 *         issuer or audience validation
	 */
	public JwtClaimAccessor verify(String token) throws AuthenticationException {
		return delegate.decode(token);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE)
				.append("id", registration.id())
				.append("name", registration.name())
				.append("issuer", registration.issuerUri())
				.toString();
	}
}
