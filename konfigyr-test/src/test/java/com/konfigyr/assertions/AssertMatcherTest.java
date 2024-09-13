package com.konfigyr.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssertMatcherTest {

	@Test
	@DisplayName("should assert object value using Hamcrest matcher assert")
	void shouldAssertObject() {
		MatcherAssert.assertThat("value", AssertMatcher.of(it -> assertThat(it).isEqualTo("value")));
	}

	@Test
	@DisplayName("should assert object value using Hamcrest matcher assert and Assert factory")
	void shouldAssertObjectUsingFactory() {
		MatcherAssert.assertThat("value", AssertMatcher.of(
				InstanceOfAssertFactories.type(String.class),
				it -> it.isEqualTo("value")
		));
	}

	@Test
	@DisplayName("should assert null value using Hamcrest matcher assert and Assert factory")
	void shouldAssertNullsUsingFactory() {
		MatcherAssert.assertThat(null, AssertMatcher.of(
				InstanceOfAssertFactories.type(String.class),
				AbstractAssert::isNull
		));
	}

	@Test
	@DisplayName("should fail to assert object value using Hamcrest matcher assert")
	void shouldFailToAssertObject() {
		assertThatThrownBy(() -> MatcherAssert.assertThat("value",
				AssertMatcher.of(it -> assertThat(it).isEqualTo("other")))
		).isInstanceOf(AssertionError.class)
				.hasMessageContaining("Assert4J assertion to not throw an assertion error")
				.hasMessageContaining("Failed to assert object: value")
				.hasMessageContaining("expected: \"other\"")
				.hasMessageContaining("but was: \"value\"");
	}

}