package com.konfigyr.crypto;

import com.konfigyr.crypto.tink.TinkKeyEncryptionKey;
import com.konfigyr.io.ByteArray;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration used to register the {@link KeyEncryptionKeyProvider} and {@link KeysetCache} Spring Beans.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(CacheAutoConfiguration.class)
@AutoConfigureBefore(CryptoAutoConfiguration.class)
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptographyAutoConfiguration {

	private final CryptoProperties properties;

	@Bean
	KeyEncryptionKeyProvider konfigyrKekProvider() {
		final ByteArray master = ByteArray.fromBase64String(properties.getMasterKey());

		// create a new master KEK using Tink AEAD primitive
		final KeyEncryptionKey kek = TinkKeyEncryptionKey
				.builder(CryptoProperties.PROVIDER_NAME)
				.from(CryptoProperties.KEK_ID, master);

		return KeyEncryptionKeyProvider.of(CryptoProperties.PROVIDER_NAME, kek);
	}

	@Bean
	@ConditionalOnBean(CacheManager.class)
	@ConditionalOnProperty(name = "konfigyr.crypto.cache", havingValue = "true", matchIfMissing = true)
	KeysetCache registryKeysetCache(CacheManager manager) {
		return new SpringKeysetCache(manager.getCache("crypto-keysets"));
	}

}
