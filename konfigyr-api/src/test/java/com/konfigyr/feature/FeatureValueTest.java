package com.konfigyr.feature;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class FeatureValueTest {

	final ObjectMapper mapper = new ObjectMapper();

	@Test
	@DisplayName("should create an unlimited feature value")
	void createUnlimitedFeatureValue() {
		final var unlimited = FeatureValue.unlimited();

		assertThat(unlimited)
				.isNotNull()
				.isInstanceOf(LimitedFeatureValue.class)
				.asInstanceOf(InstanceOfAssertFactories.type(LimitedFeatureValue.class))
				.returns(false, LimitedFeatureValue::isLimited)
				.returns(true, LimitedFeatureValue::isUnlimited)
				.satisfies(it -> assertThatIllegalStateException()
						.isThrownBy(it::get)
				);

		assertThat(unlimited)
				.isSameAs(LimitedFeatureValue.UNLIMITED)
				.hasSameHashCodeAs(FeatureValue.unlimited())
				.hasToString("FeatureValue(unlimited)");
	}

	@Test
	@DisplayName("should serialize and deserialize unlimited feature value")
	void serializeUnlimitedFeatureValue() {
		assertThat(mapper.writeValueAsString(FeatureValue.unlimited()))
				.isEqualTo("\"unlimited\"");

		assertThat(mapper.readValue("\"unlimited\"", FeatureValue.class))
				.isEqualTo(FeatureValue.unlimited());
	}

	@Test
	@DisplayName("should create a limited feature value")
	void createLimitedFeatureValue() {
		final var value = FeatureValue.limited(100);

		assertThat(value)
				.isNotNull()
				.isInstanceOf(LimitedFeatureValue.class)
				.asInstanceOf(InstanceOfAssertFactories.type(LimitedFeatureValue.class))
				.returns(100L, LimitedFeatureValue::get)
				.returns(true, LimitedFeatureValue::isLimited)
				.returns(false, LimitedFeatureValue::isUnlimited);

		assertThat(value)
				.isEqualTo(FeatureValue.limited(100))
				.hasSameHashCodeAs(FeatureValue.limited(100))
				.hasToString("FeatureValue(limit=100)");

		assertThat(value)
				.isNotEqualTo(FeatureValue.limited(10))
				.doesNotHaveSameHashCodeAs(FeatureValue.limited(10));
	}

	@Test
	@DisplayName("should serialize and deserialize limited feature value")
	void serializeLimitedFeatureValue() {
		assertThat(mapper.writeValueAsString(FeatureValue.limited(100)))
				.isEqualTo("\"100\"");

		assertThat(mapper.readValue("\"916254\"", FeatureValue.class))
				.isEqualTo(FeatureValue.limited(916254));
	}

	@Test
	@DisplayName("should fail to create a limited feature value with a zero on negative value")
	void validateLimitedFeatureValue() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> FeatureValue.limited(-1))
				.withMessage("Limit must be greater than 0")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> FeatureValue.limited(0))
				.withMessage("Limit must be greater than 0")
				.withNoCause();
	}

	@Test
	@DisplayName("should create a rate limited feature value")
	void createRateLimitedFeatureValue() {
		final var value = FeatureValue.rateLimit(100, TimeUnit.SECONDS);

		assertThat(value)
				.isNotNull()
				.isInstanceOf(RateLimitFeatureValue.class)
				.asInstanceOf(InstanceOfAssertFactories.type(RateLimitFeatureValue.class))
				.returns(100L, RateLimitFeatureValue::rate)
				.returns(DurationUnit.SECONDS, RateLimitFeatureValue::unit);

		assertThat(value)
				.isEqualTo(FeatureValue.rateLimit(100, ChronoUnit.SECONDS))
				.hasSameHashCodeAs(FeatureValue.rateLimit(100, TimeUnit.SECONDS))
				.hasToString("FeatureValue(rate=100/s)");

		assertThat(value)
				.isNotEqualTo(FeatureValue.rateLimit(100, DurationUnit.HOURS))
				.doesNotHaveSameHashCodeAs(FeatureValue.rateLimit(100, DurationUnit.HOURS));

		assertThat(value)
				.isNotEqualTo(FeatureValue.rateLimit(10, TimeUnit.SECONDS))
				.doesNotHaveSameHashCodeAs(FeatureValue.rateLimit(10, TimeUnit.SECONDS));
	}

	@Test
	@DisplayName("should serialize and deserialize rate limited feature value")
	void serializeRateLimitedFeatureValue() {
		assertThat(mapper.writeValueAsString(FeatureValue.rateLimit(100, TimeUnit.HOURS)))
				.isEqualTo("\"100/h\"");

		assertThat(mapper.readValue("\"9162/s\"", FeatureValue.class))
				.isEqualTo(FeatureValue.rateLimit(9162, TimeUnit.SECONDS));
	}

	@Test
	@DisplayName("should fail to create a rate limited feature value with a zero on negative value")
	void validateRateLimitedFeatureValue() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> FeatureValue.rateLimit(-1, TimeUnit.SECONDS))
				.withMessage("Rate limit must be greater than 0")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> FeatureValue.rateLimit(0, TimeUnit.SECONDS))
				.withMessage("Rate limit must be greater than 0")
				.withNoCause();
	}

}
