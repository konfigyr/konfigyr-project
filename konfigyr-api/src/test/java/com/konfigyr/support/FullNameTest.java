package com.konfigyr.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FullNameTest {

	@Test
	void shouldNotParseFullNameWhenBlank() {
		assertThat(FullName.parse(null)).isNull();
		assertThat(FullName.parse("")).isNull();
		assertThat(FullName.parse("   ")).isNull();
	}

	@Test
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
		assertThat(FullName.parse(value))
				.isNotNull()
				.returns(firstName, FullName::firstName)
				.returns(lastname, FullName::lastName)
				.returns(initials, FullName::initials)
				.returns(value, FullName::get);
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