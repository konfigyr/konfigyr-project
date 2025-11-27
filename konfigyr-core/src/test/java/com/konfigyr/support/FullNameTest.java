package com.konfigyr.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FullNameTest {

	@Test
	@DisplayName("should not parse full name when blank")
	void shouldNotParseFullNameWhenBlank() {
		assertThat(FullName.parse(null)).isNull();
		assertThat(FullName.parse("")).isNull();
		assertThat(FullName.parse("   ")).isNull();
	}

	@Test
	@DisplayName("should check equals and hashcode")
	void shouldCheckEqualsAndHashCode() {
		assertThat(FullName.parse("Paul Atreides"))
				.isEqualTo(FullName.of("Paul", "Atreides"))
				.isNotEqualTo(FullName.of("Stilgar", null))
				.hasSameHashCodeAs(FullName.of("Paul", "Atreides"))
				.doesNotHaveSameHashCodeAs(FullName.of("Stilgar", null))
				.hasToString("Paul Atreides");
	}

	@MethodSource("names")
	@ParameterizedTest(name = "should parse full name: \"{0}\" into \"{1}\" \"{2}\"")
	@DisplayName("should parse full name")
	void shouldParseFullName(String value, String firstName, String lastname, String initials) {
		final var name = FullName.of(firstName, lastname);

		assertThat(name)
				.isNotNull()
				.returns(firstName, FullName::firstName)
				.returns(lastname, FullName::lastName)
				.returns(initials, FullName::initials)
				.returns(value, FullName::get);
	}

	@Test
	@DisplayName("should be serialized into and from a JSON string")
	void shouldCheckJsonSerialization() {
		final var mapper = JsonMapper.shared();

		final var name = FullName.of("Paul", "Atreides");

		assertThat(mapper.writeValueAsString(name))
				.isEqualTo("\"Paul Atreides\"");

		assertThat(mapper.readValue("\"Paul Atreides\"", FullName.class))
				.isEqualTo(name);
	}


	static Stream<Arguments> names() {
		return Stream.of(
				Arguments.of(".. .", "..", ".", ".."),
				Arguments.of("Stilgar", "Stilgar", "", "S"),
				Arguments.of("Paul Atreides", "Paul",  "Atreides", "PA"),
				Arguments.of("Liet-Kynes", "Liet-Kynes", "", "L"),
				Arguments.of("Piter De Vries", "Piter", "De Vries", "PD")
		);
	}

}
