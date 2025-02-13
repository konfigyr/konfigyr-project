package com.konfigyr.crypto;

import com.konfigyr.crypto.shamir.Shamir;
import com.konfigyr.crypto.shamir.Share;
import com.konfigyr.io.ByteArray;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;

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
	 * Defines the Key Encryption Key (KEK) used by the Konfigyr application to wrap Data Encryption Keys (DEK)
	 * and unwrap encrypted Data Encryption Keys (eDEK).
	 */
	private MasterKey masterKey = new MasterKey();

	/**
	 * Enables the Keyset cache backed by Spring cache manager. Defaults to {@literal true}.
	 *
	 * @see SpringKeysetCache
	 */
	private boolean cache = true;

	/**
	 * Defines how the master key should be provided to the application.
	 */
	@Data
	public static class MasterKey {

		/**
		 * Base 64 encoded value of the Key Encryption Key (KEK) used by the Konfigyr application.
		 */
		private String value;

		/**
		 * Collection of {@link com.konfigyr.crypto.shamir.Share Shamir shares} that can be used to
		 * recover the Key Encryption Key (KEK) that is used by the Konfigyr application.
		 */
		private List<String> shares = List.of();

		@NonNull
		ByteArray get() {
			if (!CollectionUtils.isEmpty(getShares())) {
				return Shamir.getInstance().join(getShares().stream().map(Share::from).sorted().toList());
			}

			if (StringUtils.hasText(value)) {
				return ByteArray.fromBase64String(getValue());
			}

			throw new IllegalStateException("Failed to resolve Key Encryption Key (KEK), no shares or value specified");
		}

	}
}
