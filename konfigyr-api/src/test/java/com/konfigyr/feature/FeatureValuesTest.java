package com.konfigyr.feature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class FeatureValuesTest {

	static final FeatureDefinition<LimitedFeatureValue> TESTING = FeatureDefinition.of("testing", LimitedFeatureValue.class);

	@Test
	@DisplayName("should create an empty feature values instance")
	void shouldCreateEmptyFeatureValues() {
		final var values = FeatureValues.empty();

		assertThat(values)
				.isNotNull()
				.returns(true, FeatureValues::isEmpty)
				.isSameAs(FeatureValues.empty())
				.isSameAs(FeatureValues.builder().build())
				.hasToString("FeatureValues(empty)");
	}

	@Test
	@DisplayName("should create feature values instance")
	void shouldCreateFeatureValues() {
		final var values = FeatureValues.builder()
				.add("testing", FeatureValue.unlimited())
				.add((FeatureDefinition<?>) null, null)
				.add((String) null, FeatureValue.unlimited())
				.add("testing", null)
				.add(TestFeatureDefinitions.LIMITED, FeatureValue.limited(10))
				.add(Collections.emptyMap())
				.build();

		assertThat(values)
				.isNotNull()
				.returns(false, FeatureValues::isEmpty)
				.hasToString("FeatureValues(limited=%s, testing=%s)", FeatureValue.limited(10), FeatureValue.unlimited());

		assertThat(values.get(TestFeatureDefinitions.LIMITED))
				.isPresent()
				.hasValue(FeatureValue.limited(10));

		assertThat(values.get(TestFeatureDefinitions.RATE_LIMITED))
				.isEmpty();
	}

	@Test
	@DisplayName("should assert that feature value matches the definition")
	void shouldAssertValueType() {
		final var values = FeatureValues.builder()
				.add("testing", FeatureValue.rateLimit(10, TimeUnit.SECONDS))
				.build();

		assertThatIllegalStateException()
				.isThrownBy(() -> values.get(TESTING))
				.withMessageContaining("does not match the defined type")
				.withNoCause();
	}

	@Test
	@DisplayName("should concatenate feature values with an empty container")
	void shouldConcatenateFeatureValuesWithEmptyContainer() {
		final var values = FeatureValues.builder()
				.add(TESTING, FeatureValue.unlimited())
				.build();

		assertThat(values.concat(FeatureValues.empty()))
				.isSameAs(values);
	}

	@Test
	@DisplayName("should concatenate feature values with an non-empty container")
	void shouldConcatenateFeatureValues() {
		final var values = FeatureValues.builder()
				.add(TESTING, FeatureValue.unlimited())
				.add(TestFeatureDefinitions.RATE_LIMITED, FeatureValue.rateLimit(10, TimeUnit.MINUTES))
				.build();

		final var other = FeatureValues.builder()
				.add("testing", FeatureValue.limited(1))
				.add(TestFeatureDefinitions.LIMITED, FeatureValue.limited(2))
				.build();

		assertThat(values.concat(other))
				.isNotEqualTo(values)
				.isNotEqualTo(other)
				.satisfies(it -> assertThat(it.get(TESTING))
						.hasValue(FeatureValue.limited(1))
				)
				.satisfies(it -> assertThat(it.get(TestFeatureDefinitions.LIMITED))
						.hasValue(FeatureValue.limited(2))
				)
				.satisfies(it -> assertThat(it.get(TestFeatureDefinitions.RATE_LIMITED))
						.hasValue(FeatureValue.rateLimit(10, TimeUnit.MINUTES))
				);
	}

}
