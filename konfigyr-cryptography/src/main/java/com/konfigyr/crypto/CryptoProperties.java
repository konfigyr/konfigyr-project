package com.konfigyr.crypto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Spring configuration properties used to create the primary {@link KeyEncryptionKeyProvider}
 * for the Konfigyr application.
 *
 * @author Vladimir Spasic
 * @since 1.0.
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
	 * Identifier of the {@link KeyEncryptionKey Key Encryption Key (KEK)} used by the Konfigyr
	 * application to wrap {@link Keyset Data Encryption Keys (DEK)} and unwrap the
	 * {@link EncryptedKeyset encrypted Data Encryption Keys (eDEK)}.
	 */
	public static final String KEK_ID = "master";

	/**
	 * Base 64 encoded value of the Key Encryption Key (KEK) used by the Konfigyr application
	 * to wrap Data Encryption Keys (DEK) and unwrap encrypted Data Encryption Keys (eDEK).
	 */
	@NotEmpty
	private String masterKey;

	/**
	 * Enables the Keyset cache backed by Spring cache manager. Defaults to {@literal true}.
	 *
	 * @see SpringKeysetCache
	 */
	private boolean cache = true;

}
