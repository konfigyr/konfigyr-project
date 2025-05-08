package com.konfigyr.feature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class FeatureValueConverterTest {

	@DisplayName("should fail to convert invalid feature values")
	@ParameterizedTest(name = "converting invalid feature value of: {0}")
	@ValueSource(strings = { "", "   ", "invalid", "100/y", "100/ms", "100/M", "10.00/s", "123f", "123.00", "123,00" })
	void invalidFeatureValues(String value) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> FeatureValueConverter.from(value));
	}

	@Test
	@DisplayName("should convert unlimited feature value from string")
	void convertFromUnlimited() {
		assertThatObject(FeatureValueConverter.from("unlimited"))
				.isInstanceOf(LimitedFeatureValue.class)
				.isEqualTo(FeatureValue.unlimited());
	}

	@Test
	@DisplayName("should convert unlimited feature value to string")
	void convertToUnlimited() {
		assertThatObject(FeatureValueConverter.to(FeatureValue.unlimited()))
				.isEqualTo("unlimited");
	}

	@Test
	@DisplayName("should convert limited feature value from string")
	void convertFromLimited() {
		assertThatObject(FeatureValueConverter.from("192562"))
				.isInstanceOf(LimitedFeatureValue.class)
				.isEqualTo(FeatureValue.limited(192562));
	}

	@Test
	@DisplayName("should convert limited feature value to string")
	void convertToLimited() {
		assertThatObject(FeatureValueConverter.to(FeatureValue.limited(123)))
				.isEqualTo("123");
	}

	@Test
	@DisplayName("should convert rate limited feature value from string")
	void convertFromRateLimited() {
		assertThatObject(FeatureValueConverter.from("100/m"))
				.isInstanceOf(RateLimitFeatureValue.class)
				.isEqualTo(FeatureValue.rateLimit(100, TimeUnit.MINUTES));
	}

	@Test
	@DisplayName("should convert rate limited feature value to string")
	void convertToRateLimited() {
		assertThatObject(FeatureValueConverter.to(FeatureValue.rateLimit(100, TimeUnit.SECONDS)))
				.isEqualTo("100/s");
	}

}
