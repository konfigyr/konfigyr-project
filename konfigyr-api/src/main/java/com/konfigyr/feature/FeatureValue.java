package com.konfigyr.feature;

import org.jspecify.annotations.NonNull;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Type that represents a value which one {@link FeatureDefinition} can have.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@JsonSerialize(using = FeatureValueConverter.Serializer.class)
@JsonDeserialize(using = FeatureValueConverter.Deserializer.class)
public sealed interface FeatureValue extends Serializable permits LimitedFeatureValue, RateLimitFeatureValue {

	/**
	 * Create a {@link FeatureValue} that represents an unlimited value. Usually used when the
	 * {@link FeatureDefinition feature} should not apply any restrictions.
	 *
	 * @return unlimited feature value, never {@literal null}
	 */
	@NonNull
	static LimitedFeatureValue unlimited() {
		return LimitedFeatureValue.UNLIMITED;
	}

	/**
	 * Create a {@link FeatureValue} that represents a certain limit. One example can be that one
	 * {@link com.konfigyr.namespace.Namespace} can only have a certain amount of members or vaults
	 * depending on the selected subscription plan.
	 *
	 * @param limit the limit must be a positive number
	 * @return limited feature value, never {@literal null}
	 */
	static LimitedFeatureValue limited(long limit) {
		return new LimitedFeatureValue(limit);
	}

	/**
	 * Create a {@link FeatureValue} that represents how much operations should be performed during the
	 * specified time range. One example of rate limiting could be how much API requests can be sent per
	 * second or minute.
	 *
	 * @param value the limit must be a positive number
	 * @param unit the time unit, can't be {@literal null}
	 * @return rate limited feature value, never {@literal null}
	 */
	static RateLimitFeatureValue rateLimit(long value, @NonNull TimeUnit unit) {
		return rateLimit(value, DurationUnit.from(unit));
	}

	/**
	 * Create a {@link FeatureValue} that represents how much operations should be performed during the
	 * specified time range. One example of rate limiting could be how much API requests can be sent per
	 * second or minute.
	 *
	 * @param value the limit must be a positive number
	 * @param unit the chrono-unit, can't be {@literal null}
	 * @return rate limited feature value, never {@literal null}
	 */
	static RateLimitFeatureValue rateLimit(long value, @NonNull ChronoUnit unit) {
		return rateLimit(value, DurationUnit.from(unit));
	}

	/**
	 * Create a {@link FeatureValue} that represents how much operations should be performed during the
	 * specified time range. One example of rate limiting could be how much API requests can be sent per
	 * second or minute.
	 *
	 * @param value the limit must be a positive number
	 * @param unit the duration unit, can't be {@literal null}
	 * @return rate limited feature value, never {@literal null}
	 */
	static RateLimitFeatureValue rateLimit(long value, @NonNull DurationUnit unit) {
		return new RateLimitFeatureValue(value, unit);
	}

}
