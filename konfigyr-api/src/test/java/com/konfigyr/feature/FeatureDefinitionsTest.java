package com.konfigyr.feature;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FeatureDefinitionsTest {

	final FeatureDefinitions definitions = new FeatureDefinitions(
			TestFeatureDefinitions.LIMITED,
			TestFeatureDefinitions.RATE_LIMITED
	);

	@Test
	@DisplayName("should make sure that there are no duplicate features definitions")
	void shouldCheckForDuplicates() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FeatureDefinitions(TestFeatureDefinitions.LIMITED, TestFeatureDefinitions.LIMITED))
				.withMessage("You have attempted to register a Feature Definition that is already registered: %s", TestFeatureDefinitions.LIMITED)
				.withNoCause();
	}

	@Test
	@DisplayName("should iterate through registered feature definitions")
	void shouldIterateThroughValues() {
		assertThat(definitions)
				.isNotNull()
				.hasSize(2)
				.containsExactly(
						TestFeatureDefinitions.LIMITED,
						TestFeatureDefinitions.RATE_LIMITED
				);
	}

	@Test
	@DisplayName("should create a stream of registered feature definitions")
	void shouldStreamThroughValues() {
		assertThat(definitions.stream())
				.isNotNull()
				.hasSize(2)
				.isSortedAccordingTo(FeatureDefinition::compareTo)
				.containsExactly(
						TestFeatureDefinitions.LIMITED,
						TestFeatureDefinitions.RATE_LIMITED
				);
	}

	@Test
	@DisplayName("should retrieve registered feature definition by name")
	void shouldLookupByName() {
		assertThat(definitions.get(TestFeatureDefinitions.LIMITED.name()))
				.isPresent()
				.get(InstanceOfAssertFactories.type(FeatureDefinition.class))
				.isEqualTo(TestFeatureDefinitions.LIMITED);
	}

	@Test
	@DisplayName("should return an empty optional for unregistered feature definition")
	void shouldLookupUnknownFeatureDefinition() {
		assertThat(definitions.get("unknown"))
				.isEmpty();
	}

	@Test
	@DisplayName("should check if feature definition is registered")
	void shouldCheckFeatureDefinitionRegistration() {
		assertThat(definitions.has(TestFeatureDefinitions.RATE_LIMITED)).isTrue();
		assertThat(definitions.has("unknown")).isFalse();
	}

	@Test
	@DisplayName("should generate if feature definitions are equal or if they have same identity")
	void shouldCheckFeatureDefinitionIdentity() {
		final var same = new FeatureDefinitions(TestFeatureDefinitions.RATE_LIMITED, TestFeatureDefinitions.LIMITED);
		final var other = new FeatureDefinitions(TestFeatureDefinitions.RATE_LIMITED);

		assertThatObject(definitions)
				.isEqualTo(same)
				.isNotEqualTo(other);

		assertThatObject(definitions)
				.hasSameHashCodeAs(same)
				.doesNotHaveSameHashCodeAs(other);
	}

	@Test
	@DisplayName("should feature definitions string representation")
	void shouldGenerateStringRepresentation() {
		assertThatObject(definitions)
				.hasToString("FeatureDefinitions(limited, rate-limited)");
	}

}
