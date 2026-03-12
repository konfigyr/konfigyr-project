package com.konfigyr.data.converter;

import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.support.Slug;
import org.jooq.Converter;
import org.jooq.exception.DataTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonByteArrayConverterTest {

	@Test
	@DisplayName("should convert using default object mapper")
	void shouldConvertWithDefaultObjectMapper() {
		final var id = EntityId.from(4L);
		final var bytes = ByteArray.fromString("\"" + id.serialize() + "\"");

		assertThat(JsonByteArrayConverter.create(EntityId.class))
				.returns(EntityId.class, Converter::toType)
				.returns(ByteArray.class, Converter::fromType)
				.returns(null, converter -> converter.from(null))
				.returns(null, converter -> converter.to(null))
				.returns(id, converter -> converter.from(bytes))
				.returns(bytes, converter -> converter.to(id));
	}

	@Test
	@DisplayName("should convert using custom object mapper")
	void shouldConvertWithCustomObjectMapper() {
		final var bytes = ByteArray.fromString("{\"name\":\"John Doe\"}");
		final var value = Map.of("name", "John Doe");

		assertThat(JsonByteArrayConverter.create(new ObjectMapper(), Map.class))
				.returns(Map.class, Converter::toType)
				.returns(ByteArray.class, Converter::fromType)
				.returns(null, converter -> converter.from(null))
				.returns(null, converter -> converter.to(null))
				.returns(value, converter -> converter.from(bytes))
				.returns(bytes, converter -> converter.to(value));
	}

	@Test
	@DisplayName("should catch JSON processing errors")
	void shouldCatchJsonProcessingErrors() {
		assertThatThrownBy(() -> JsonByteArrayConverter.create(Slug.class).from(ByteArray.fromString("some slug")))
				.isInstanceOf(DataTypeException.class)
				.hasMessageContaining("Error when converting JSON to class")
				.hasCauseInstanceOf(JacksonException.class);
	}

}
