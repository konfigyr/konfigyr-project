package com.konfigyr.crypto;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.konfigyr.crypto.Stubs.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeysetOperationsTest {

	@Mock
	Keyset keyset;

	@Test
	@DisplayName("should fail to perform operations when supplied keyset is null")
	void shouldAssertSuppliedKeyset() {
		final var operations = KeysetOperations.of(() -> null);

		assertMissingKeyset(() -> operations.encrypt(DATA));
		assertMissingKeyset(() -> operations.encrypt(DATA, CONTEXT));
		assertMissingKeyset(() -> operations.decrypt(DATA));
		assertMissingKeyset(() -> operations.decrypt(DATA, CONTEXT));
		assertMissingKeyset(() -> operations.sign(DATA));
		assertMissingKeyset(() -> operations.verify(RESULT, DATA));

		verifyNoInteractions(keyset);
	}

	@Test
	@DisplayName("should encrypt data without authentication context")
	void shouldEncrypt() {
		doReturn(RESULT).when(keyset).encrypt(DATA);

		assertThat(KeysetOperations.of(keyset))
				.extracting(operations -> operations.encrypt(DATA))
				.isEqualTo(RESULT);

		verify(keyset).encrypt(DATA);
	}

	@Test
	@DisplayName("should encrypt data with authentication context")
	void shouldEncryptWithContext() {
		doReturn(RESULT).when(keyset).encrypt(DATA, CONTEXT);

		assertThat(KeysetOperations.of(keyset))
				.extracting(operations -> operations.encrypt(DATA, CONTEXT))
				.isEqualTo(RESULT);

		verify(keyset).encrypt(DATA, CONTEXT);
	}

	@Test
	@DisplayName("should decrypt data without authentication context")
	void shouldDecrypt() {
		doReturn(RESULT).when(keyset).decrypt(DATA);

		assertThat(KeysetOperations.of(keyset))
				.extracting(operations -> operations.decrypt(DATA))
				.isEqualTo(RESULT);

		verify(keyset).decrypt(DATA);
	}

	@Test
	@DisplayName("should decrypt data with authentication context")
	void shouldDecryptWithContext() {
		doReturn(RESULT).when(keyset).decrypt(DATA, CONTEXT);

		assertThat(KeysetOperations.of(keyset))
				.extracting(operations -> operations.decrypt(DATA, CONTEXT))
				.isEqualTo(RESULT);

		verify(keyset).decrypt(DATA, CONTEXT);
	}

	@Test
	@DisplayName("should sign data")
	void shouldSign() {
		doReturn(RESULT).when(keyset).sign(DATA);

		assertThat(KeysetOperations.of(keyset))
				.extracting(operations -> operations.sign(DATA))
				.isEqualTo(RESULT);

		verify(keyset).sign(DATA);
	}

	@Test
	@DisplayName("should verify signature")
	void shouldVerify() {
		doReturn(true).when(keyset).verify(RESULT, DATA);

		assertThat(KeysetOperations.of(keyset))
				.extracting(operations -> operations.verify(RESULT, DATA))
				.isEqualTo(true);

		verify(keyset).verify(RESULT, DATA);
	}

	static void assertMissingKeyset(ThrowableAssert.ThrowingCallable operation) {
		assertThatThrownBy(operation)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Delegating keyset for operations can not be null");
	}
}
