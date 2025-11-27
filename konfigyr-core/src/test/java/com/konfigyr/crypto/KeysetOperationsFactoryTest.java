package com.konfigyr.crypto;

import com.konfigyr.crypto.tink.TinkAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.DisposableBean;

import static com.konfigyr.crypto.Stubs.DATA;
import static com.konfigyr.crypto.Stubs.RESULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeysetOperationsFactoryTest {

	@Mock
	Keyset keyset;

	@Mock
	KeysetStore store;

	@Mock
	KeyEncryptionKey kek;

	KeysetDefinition definition;
	KeysetOperationsFactory factory;

	@BeforeEach
	void setup() {
		definition = KeysetDefinition.of("test-keyset", TinkAlgorithm.AES128_GCM);
		factory = new KonfigyrKeysetOperationsFactory(store);
	}

	@Test
	@DisplayName("should create operations where keyset already exists in the store")
	void shouldCreateOperationsWithExistingKeyset() {
		doReturn(keyset).when(store).read(definition.getName());
		doReturn(RESULT).when(keyset).encrypt(DATA);

		assertThat(factory.create(definition))
				.isNotNull()
				.extracting(operations -> operations.encrypt(DATA))
				.isEqualTo(RESULT);

		verify(store).read(definition.getName());
		verify(keyset).encrypt(DATA);

		verify(store, times(0)).kek(any(), any());
		verify(store, times(0)).create(any(), eq(definition));
	}

	@Test
	@DisplayName("should create operations where keyset does not exist in the store")
	void shouldCreateOperationsWithNewKeyset() {
		doThrow(CryptoException.KeysetNotFoundException.class).when(store).read(definition.getName());
		doReturn(kek).when(store).kek(CryptoProperties.PROVIDER_NAME, CryptoProperties.KEK_ID);
		doReturn(keyset).when(store).create(kek, definition);
		doReturn(RESULT).when(keyset).encrypt(DATA);

		assertThat(factory.create(definition))
				.isNotNull()
				.extracting(operations -> operations.encrypt(DATA))
				.isEqualTo(RESULT);

		verify(store).read(definition.getName());
		verify(store).kek(CryptoProperties.PROVIDER_NAME, CryptoProperties.KEK_ID);
		verify(store).create(kek, definition);
		verify(keyset).encrypt(DATA);
	}

	@Test
	@DisplayName("should cache created operations by keyset definition")
	void shouldCacheOperations() throws Exception {
		final var operations = factory.create(definition);

		assertThat(operations)
				.isEqualTo(factory.create(definition))
				.isNotEqualTo(factory.create(KeysetDefinition.of("test-keyset", TinkAlgorithm.AES256_GCM)));

		((DisposableBean) factory).destroy();

		assertThat(operations)
				.isNotEqualTo(factory.create(definition));
	}

	@Test
	@DisplayName("should create operations where keyset is lazily retrieved")
	void shouldCreateOperationsWithSupplier() {
		doReturn(keyset).when(store).read(definition.getName());
		doReturn(RESULT).when(keyset).encrypt(DATA);

		final var operations = factory.create(definition);

		verifyNoInteractions(store);
		verifyNoInteractions(keyset);

		assertThatNoException().isThrownBy(() -> operations.encrypt(DATA));

		verify(store).read(definition.getName());
		verify(keyset).encrypt(DATA);

		assertThatNoException().isThrownBy(() -> operations.encrypt(DATA));

		verify(store, times(2)).read(definition.getName());
		verify(keyset, times(2)).encrypt(DATA);
	}
}
