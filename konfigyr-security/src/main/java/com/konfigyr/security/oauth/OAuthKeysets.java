package com.konfigyr.security.oauth;

import com.konfigyr.crypto.KeysetDefinition;
import com.konfigyr.crypto.tink.TinkAlgorithm;

import java.time.Duration;

/**
 * Interface that contains the {@link KeysetDefinition keyset definitions} used by the
 * Spring OAuth.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@SuppressWarnings("checkstyle:InterfaceIsType")
public interface OAuthKeysets {

	/**
	 * Keyset that uses the {@link TinkAlgorithm#AES128_CTR_HMAC_SHA256} algorithm to
	 * encrypt the issued OAuth2 access tokens before they are stored in the database.
	 * <p>
	 * The keys within this {@link com.konfigyr.crypto.Keyset} should be rotated on a
	 * {@code 90 day} period.
	 */
	KeysetDefinition ACCESS_TOKEN = KeysetDefinition.of("oauth-access-token-secret",
			TinkAlgorithm.AES128_CTR_HMAC_SHA256, Duration.ofDays(90));

}
