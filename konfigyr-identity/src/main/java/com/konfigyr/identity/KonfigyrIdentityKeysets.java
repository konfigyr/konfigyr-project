package com.konfigyr.identity;

import com.konfigyr.crypto.KeysetDefinition;
import com.konfigyr.crypto.KeysetPurpose;
import com.konfigyr.crypto.jose.JoseAlgorithm;
import com.konfigyr.crypto.tink.TinkAlgorithm;

import java.time.Duration;

/**
 * Interface that contains the {@link KeysetDefinition keyset definitions} used by the
 * Konfigyr Identity Service.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@SuppressWarnings("checkstyle:InterfaceIsType")
public interface KonfigyrIdentityKeysets {

	/**
	 * Keyset that uses the {@link TinkAlgorithm#AES128_CTR_HMAC_SHA256} algorithm to encrypt and decrypt
	 * {@link org.springframework.security.oauth2.client.OAuth2AuthorizedClient Authorized OAuth 2.0 clients}.
	 * <p>
	 * The keys within this {@link com.konfigyr.crypto.Keyset} should be rotated on a {@code 90 day} period.
	 */
	KeysetDefinition AUTHORIZED_CLIENTS = KeysetDefinition.builder()
			.name("oauth-authorized-clients")
			.purpose(KeysetPurpose.ENCRYPTION)
			.algorithm(TinkAlgorithm.AES128_CTR_HMAC_SHA256)
			.rotationInterval(Duration.ofDays(90))
			.build();

	/**
	 * Keyset that uses the {@link TinkAlgorithm#AES128_CTR_HMAC_SHA256} algorithm to encrypt and decrypt
	 * {@link org.springframework.security.oauth2.server.authorization.OAuth2Authorization OAuth 2.0 Authorizations}.
	 * <p>
	 * The keys within this {@link com.konfigyr.crypto.Keyset} should be rotated on a {@code 90 day} period.
	 */
	KeysetDefinition AUTHORIZATIONS = KeysetDefinition.builder()
			.name("oauth-authorizations")
			.purpose(KeysetPurpose.ENCRYPTION)
			.algorithm(TinkAlgorithm.AES128_CTR_HMAC_SHA256)
			.rotationInterval(Duration.ofDays(90))
			.build();

	/**
	 * Keyset that uses the {@link JoseAlgorithm#RS256} algorithm to encrypt and decrypt
	 * {@link com.nimbusds.jose.jwk.JWK JSON Web Keys}.
	 * <p>
	 * The keys within this {@link com.konfigyr.crypto.Keyset} should be rotated on a {@code 180 day} period.
	 */
	KeysetDefinition WEB_KEYS = KeysetDefinition.builder()
			.name("oauth-keys")
			.purpose(KeysetPurpose.SIGNING)
			.algorithm(JoseAlgorithm.PS256)
			.rotationInterval(Duration.ofDays(180))
			.build();

}
