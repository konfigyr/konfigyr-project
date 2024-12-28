package com.konfigyr.data.converter;

import com.konfigyr.crypto.CryptoException;
import com.konfigyr.crypto.Keyset;
import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.io.ByteArray;
import org.jooq.Converter;
import org.jooq.exception.DataTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionConverterTest {

	static ByteArray encrypted = ByteArray.fromString("encrypted");
	static ByteArray decrypted = ByteArray.fromString("decrypted");

	@Mock
	Keyset keyset;

	Converter<ByteArray, ByteArray> converter;

	@BeforeEach
	void setup() {
		converter = EncryptionConverter.create(KeysetOperations.of(keyset));
	}

	@Test
	@DisplayName("should encrypt byte array")
	void shouldEncryptByteArray() {
		doReturn(decrypted).when(keyset).encrypt(encrypted);

		assertThat(converter.to(encrypted))
				.isNotNull()
				.isEqualTo(decrypted);
	}

	@Test
	@DisplayName("should encrypt null or empty byte array")
	void shouldEncryptEmptyByteArray() {
		assertThat(converter.to(null)).isNull();

		assertThat(converter.to(ByteArray.empty()))
				.isNotNull()
				.isSameAs(ByteArray.empty());

		verifyNoInteractions(keyset);
	}

	@Test
	@DisplayName("should decrypt byte array")
	void shouldDecryptByteArray() {
		doReturn(encrypted).when(keyset).decrypt(decrypted);

		assertThat(converter.from(decrypted))
				.isNotNull()
				.isEqualTo(encrypted);
	}

	@Test
	@DisplayName("should decrypt null or empty byte array")
	void shouldDecryptEmptyByteArray() {
		assertThat(converter.from(null)).isNull();

		assertThat(converter.from(ByteArray.empty()))
				.isNotNull()
				.isSameAs(ByteArray.empty());

		verifyNoInteractions(keyset);
	}

	@Test
	@DisplayName("should catch crypto exceptions")
	void shouldCatchCryptoExceptions() {
		doThrow(CryptoException.KeysetOperationException.class).when(keyset).encrypt(encrypted);
		doThrow(CryptoException.KeysetOperationException.class).when(keyset).decrypt(decrypted);

		assertThatThrownBy(() -> converter.to(encrypted))
				.isInstanceOf(DataTypeException.class)
				.hasMessageContaining("Error when encrypting data")
				.hasCauseInstanceOf(CryptoException.class);

		assertThatThrownBy(() -> converter.from(decrypted))
				.isInstanceOf(DataTypeException.class)
				.hasMessageContaining("Error when decrypting data")
				.hasCauseInstanceOf(CryptoException.class);
	}

}