package com.konfigyr.data.converter;

import com.konfigyr.io.ByteArray;
import org.jooq.Converter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageDigestConverterTest {

	static ByteArray value = ByteArray.fromString("value to be hashed");
	static ByteArray digest = ByteArray.fromBase64String("lEsH5gLfsuHsl3Sl72uluKrNuPOHATczBn-JUc5chTg=");

	Converter<ByteArray, ByteArray> converter;

	@BeforeEach
	void setup() {
		converter = MessageDigestConverter.create("SHA-256");
	}

	@Test
	@DisplayName("should perform message digest on the byte array")
	void shouldDigestByteArray() {
		assertThat(converter.to(value))
				.isNotNull()
				.isEqualTo(digest);
	}

	@Test
	@DisplayName("should digest null or empty byte array")
	void shouldDigestEmptyByteArray() {
		assertThat(converter.to(null))
				.isNull();

		assertThat(converter.to(ByteArray.empty()))
				.isNotNull()
				.isSameAs(ByteArray.empty());
	}

	@Test
	@DisplayName("should not perform any digest operations on database values")
	void shouldDecryptByteArray() {
		assertThat(converter.from(null))
				.isNull();

		assertThat(converter.from(ByteArray.empty()))
				.isNotNull()
				.isSameAs(ByteArray.empty());

		assertThat(converter.from(digest))
				.isNotNull()
				.isSameAs(digest);
	}

	@Test
	@DisplayName("should catch exceptions while creating message digest")
	void shouldExceptionsWhileCreatingMessageDigest() {
		assertThatThrownBy(() -> MessageDigestConverter.create("unknown"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Could not create converter as digest algorithm is not supported")
				.hasCauseInstanceOf(NoSuchAlgorithmException.class);

		assertThatThrownBy(() -> MessageDigestConverter.create("unknown", "unknown"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Could not create converter as digest algorithm is not supported")
				.hasCauseInstanceOf(NoSuchProviderException.class);

		assertThatThrownBy(() -> MessageDigestConverter.create("SHA-256", "unknown"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Could not create converter as security provider is not supported")
				.hasCauseInstanceOf(NoSuchProviderException.class);
	}

}
