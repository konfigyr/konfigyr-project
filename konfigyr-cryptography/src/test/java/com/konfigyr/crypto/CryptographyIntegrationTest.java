package com.konfigyr.crypto;

import com.konfigyr.crypto.tink.TinkAlgorithm;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.cache.annotation.EnableCaching;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@TestProfile
@ImportTestcontainers(TestContainers.class)
@SpringBootTest(classes = CryptographyIntegrationTest.Configuration.class)
public class CryptographyIntegrationTest {

	@Autowired
	KeysetStore store;

	@Autowired
	KeysetOperationsFactory factory;

	@SpyBean
	KeysetRepository repository;

	@Test
	@DisplayName("should create keyset that only performs encryption operation")
	void encryption() throws Exception {
		final var definition = KeysetDefinition.of("test-encryption", TinkAlgorithm.AES128_GCM);
		final var operations = factory.create(definition);

		assertThat(operations.encrypt(Stubs.DATA))
				.extracting(operations::decrypt)
				.isEqualTo(Stubs.DATA);

		assertThat(operations.encrypt(Stubs.DATA, Stubs.CONTEXT))
				.extracting(cipher -> operations.decrypt(cipher, Stubs.CONTEXT))
				.isEqualTo(Stubs.DATA);

		assertThatThrownBy(() -> operations.sign(Stubs.DATA))
				.isInstanceOf(CryptoException.UnsupportedKeysetOperationException.class);

		verify(repository).read(definition.getName());
		verify(repository).write(any());
	}

	@Test
	@DisplayName("should use existing keyset that only performs signing operation")
	void signing() throws Exception {
		final var definition = KeysetDefinition.of("test-signing", TinkAlgorithm.ECDSA_P256);

		assertThatNoException().isThrownBy(() -> store.create(
				CryptoProperties.PROVIDER_NAME,
				CryptoProperties.KEK_ID,
				definition
		));

		final var operations = factory.create(definition);

		assertThat(operations.sign(Stubs.DATA))
				.extracting(signature -> operations.verify(signature, Stubs.DATA))
				.isEqualTo(true);

		assertThatThrownBy(() -> operations.encrypt(Stubs.DATA))
				.isInstanceOf(CryptoException.UnsupportedKeysetOperationException.class);

		// once the key is written, it is stored in cache, that's why read should not be invoked
		verify(repository).write(any());
		verify(repository, times(0)).read(definition.getName());
	}

	@EnableCaching
	@EnableAutoConfiguration
	static class Configuration {

	}

}
