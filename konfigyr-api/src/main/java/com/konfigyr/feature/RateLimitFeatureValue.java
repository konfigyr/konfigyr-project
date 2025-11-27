package com.konfigyr.feature;

import lombok.EqualsAndHashCode;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.io.Serial;

/**
 * Implementation of a {@link FeatureValue} that represents how much operations should be performed
 * during the specified time range. One example of rate limiting could be how much API requests can be sent per
 * second or minute.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@EqualsAndHashCode
public final class RateLimitFeatureValue implements FeatureValue {

	@Serial
	private static final long serialVersionUID = 7402037206503248216L;

	private final long rate;

	private final DurationUnit unit;

	RateLimitFeatureValue(long rate, DurationUnit unit) {
		Assert.isTrue(rate > 0, "Rate limit must be greater than 0");
		Assert.notNull(unit, "Rate limit time unit cannot be null");

		this.rate = rate;
		this.unit = unit;
	}

	public long rate() {
		return rate;
	}

	@NonNull
	public DurationUnit unit() {
		return unit;
	}

	@Override
	public String toString() {
		return "FeatureValue(rate=" + rate + "/" + unit.symbol() + ")";
	}

}
