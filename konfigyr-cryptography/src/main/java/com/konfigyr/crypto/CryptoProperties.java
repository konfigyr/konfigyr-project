package com.konfigyr.crypto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Spring configuration properties used to create the primary {@link KeyEncryptionKeyProvider}
 * for the Konfigyr application.
 *
 * @author : Vladimir Spasic
 * @since : 23.10.23, Mon
 **/
@Data
@Validated
@ConfigurationProperties(prefix = "konfigyr.crypto")
public class CryptoProperties {

	/**
	 * Name of the {@link KeyEncryptionKeyProvider} used by the Konfigyr application.
	 */
	public static final String PROVIDER_NAME = "konfigyr-registry";

	/**
	 * Identifier of the {@link KeyEncryptionKey} used by the Konfigyr application to
	 * wrap and unwrap {@link Keyset data encryption keys}.
	 */
	public static final String KEK_ID = "master";

	/**
	 * The {@link KeyEncryptionKey} value that is used to wrap and unwrap the {@link Keyset data encryption keys}.
	 */
	@NotEmpty
	private String masterKey;

	/**
	 * Enables the {@link KeysetCache} backed by the {@link org.springframework.cache.CacheManager}.
	 * <p>
	 * Defaults to {@literal true}.
	 *
	 * @see SpringKeysetCache
	 */
	private boolean cache = true;

}
