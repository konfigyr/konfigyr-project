package com.konfigyr.data.converter;

import com.konfigyr.io.ByteArray;
import org.apache.commons.lang3.RandomUtils;
import org.jooq.Converter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ByteArrayConverterTest {

	final Converter<byte[], ByteArray> converter = ByteArrayConverter.getInstance();

	@Test
	@DisplayName("should define convertible types")
	void shouldDefineConvertibleTypes() {
		assertThat(converter)
				.isSameAs(ByteArrayConverter.getInstance())
				.returns(ByteArray.class, Converter::toType)
				.returns(byte.class.arrayType(), Converter::fromType);
	}

	@Test
	@DisplayName("should convert from primitive byte arrays")
	void shouldConvertFromPrimitiveByteArrays() {
		final var bytes = generateRandomByteArray(16);

		assertThat(converter.from(bytes))
				.isNotNull()
				.isInstanceOf(ByteArray.class)
				.extracting(ByteArray::array)
				.isEqualTo(bytes);
	}

	@Test
	@DisplayName("should convert from null or empty primitive byte arrays")
	void shouldConvertFromEmptyPrimitiveByteArrays() {
		assertThat(converter.from(null))
				.isNull();

		assertThat(converter.from(generateRandomByteArray(0)))
				.isNotNull()
				.isInstanceOf(ByteArray.class)
				.isSameAs(ByteArray.empty());
	}

	@Test
	@DisplayName("should convert to primitive byte arrays")
	void shouldConvertToPrimitiveByteArrays() {
		final var bytes = generateRandomByteArray(32);

		assertThat(converter.to(new ByteArray(bytes)))
				.isNotNull()
				.isEqualTo(bytes);
	}

	@Test
	@DisplayName("should convert to null or empty primitive byte arrays")
	void shouldConvertToEmptyPrimitiveByteArrays() {
		assertThat(converter.to(null))
				.isNull();

		assertThat(converter.to(ByteArray.empty()))
				.isNotNull()
				.isEmpty();
	}

	@SuppressWarnings("deprecation")
	static byte[] generateRandomByteArray(int length) {
		return RandomUtils.nextBytes(length);
	}

}
