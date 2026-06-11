package com.konfigyr.identity.authorization.issuer;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;

/**
 * Strategy interface that resolves a {@link TrustedIssuer} capable of verifying JWT
 * subject tokens during an OAuth 2.0 Token Exchange (RFC 8693).
 * <p>
 * A trusted issuer represents an external OIDC identity provider whose JWTs this
 * authorization server accepts as the {@code subject_token} in a token exchange request.
 * Before a workload application can exchange an external token for a Konfigyr access
 * token, the registry must confirm that the token's {@code iss} claim identifies a
 * provider that the namespace has explicitly trusted.
 * <p>
 * The resolved {@link TrustedIssuer} verifies the subject token by:
 * <ul>
 *   <li>validating the JWT signature against the issuer's public signing keys, fetched
 *       from the JWKS endpoint declared in the {@link TrustedIssuerRegistration}</li>
 *   <li>asserting that the {@code iss} claim matches the registered issuer URI</li>
 *   <li>asserting that the token has not expired ({@code exp} / {@code nbf})</li>
 *   <li>when the {@link TrustedIssuerRegistration} declares allowed audiences, asserting
 *       that at least one appears in the token's {@code aud} claim</li>
 * </ul>
 * Implementations must throw
 * {@link org.springframework.security.oauth2.core.OAuth2AuthenticationException} with
 * error code {@code invalid_client} when no registration is found for the given namespace
 * and issuer URI, so that the caller can treat an unresolved issuer as an authentication
 * failure.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@FunctionalInterface
public interface TrustedIssuerRegistry {

	/**
	 * Resolves the {@link TrustedIssuer} for the given namespace and issuer URI.
	 *
	 * @param namespace the namespace on whose behalf the lookup is performed
	 * @param issuerUri the OIDC issuer URI identifying the external identity provider
	 * @return a {@link TrustedIssuer} ready to verify subject tokens, never {@code null}
	 * @throws org.springframework.security.oauth2.core.OAuth2AuthenticationException with
	 *         {@code invalid_client} if no trusted issuer is registered for the combination
	 */
	TrustedIssuer get(EntityId namespace, String issuerUri);

}
