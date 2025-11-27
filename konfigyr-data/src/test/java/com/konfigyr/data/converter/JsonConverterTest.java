package com.konfigyr.data.converter;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Slug;
import org.jooq.Converter;
import org.jooq.exception.DataTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JsonConverterTest {

	@Test
	@DisplayName("should convert using default object mapper")
	void shouldConvertWithDefaultObjectMapper() {
		final var id = EntityId.from(4L);

		assertThat(JsonConverter.create(EntityId.class))
				.returns(EntityId.class, Converter::toType)
				.returns(String.class, Converter::fromType)
				.returns(null, converter -> converter.from(null))
				.returns(null, converter -> converter.to(null))
				.returns(id, converter -> converter.from(StringNode.valueOf(id.serialize()).toString()))
				.returns(StringNode.valueOf(id.serialize()).toString(), converter -> converter.to(id));
	}

	@Test
	@DisplayName("should convert using custom object mapper")
	void shouldConvertWithCustomObjectMapper() {
		final var value = Map.of("name", "John Doe");

		assertThat(JsonConverter.create(new ObjectMapper(), Map.class))
				.returns(Map.class, Converter::toType)
				.returns(String.class, Converter::fromType)
				.returns(null, converter -> converter.from(null))
				.returns(null, converter -> converter.to(null))
				.returns(value, converter -> converter.from("{\"name\":\"John Doe\"}"))
				.returns("{\"name\":\"John Doe\"}", converter -> converter.to(value));
	}

	@Test
	@DisplayName("should catch JSON processing errors")
	void shouldCatchJsonProcessingErrors() {
		assertThatThrownBy(() -> JsonConverter.create(Slug.class).from("some slug"))
				.isInstanceOf(DataTypeException.class)
				.hasMessageContaining("Error when converting JSON to class")
				.hasCauseInstanceOf(JacksonException.class);
	}

}
