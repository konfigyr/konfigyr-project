package com.konfigyr.crypto;

import com.konfigyr.crypto.tink.TinkKeyEncryptionKey;
import com.konfigyr.io.ByteArray;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration used to register the {@link KeyEncryptionKeyProvider}, {@link KeysetCache}
 * and {@link KeysetOperationsFactory} Spring Beans.
 *
 * @author : Vladimir Spasic
 * @since : 27.03.24, Wed
 **/
@AutoConfiguration
@RequiredArgsConstructor
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

	@ConditionalOnBean(KeysetStore.class)
	@Configuration(proxyBeanMethods = false)
	static class KeysetOperationsConfiguration {

		@Bean
		@ConditionalOnMissingBean(KeysetOperationsFactory.class)
		KeysetOperationsFactory konfigyrKeysetOperationsFactory(KeysetStore store) {
			return new KonfigyrKeysetOperationsFactory(store);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class KeysetCacheConfiguration {

		@Bean
		@ConditionalOnProperty(name = "konfigyr.crypto.cache", havingValue = "true", matchIfMissing = true)
		KeysetCache registryKeysetCache(CacheManager manager) {
			return new SpringKeysetCache(manager.getCache("crypto-keysets"));
		}

	}

}
