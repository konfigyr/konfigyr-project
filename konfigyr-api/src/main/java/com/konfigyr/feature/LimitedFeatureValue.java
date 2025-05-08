package com.konfigyr.feature;

import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

import java.io.Serial;

/**
 * Implementation of a {@link FeatureValue} that represents a numeric value, usually used for defining certain
 * usage limitations. One example can be that one {@link com.konfigyr.namespace.Namespace} can only have a
 * certain amount of members or vaults depending on the selected subscription plan.
 * <p>
 * The {@link LimitedFeatureValue} can also be created without a value that is also referred to as an unlimited
 * value. Unlimited feature values are usually used when the {@link FeatureDefinition feature} should not apply
 * any limitations or restrictions.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@EqualsAndHashCode
public final class LimitedFeatureValue implements FeatureValue {

	@Serial
	private static final long serialVersionUID = 7290212079691462610L;

	static final LimitedFeatureValue UNLIMITED = new LimitedFeatureValue(null);

	private final Long limit;

	LimitedFeatureValue(Long limit) {
		Assert.isTrue(limit == null || limit > 0, "Limit must be greater than 0");
		this.limit = limit;
	}

	public boolean isLimited() {
		return limit != null;
	}

	public boolean isUnlimited() {
		return limit == null;
	}

	public long get() {
		Assert.state(limit != null, "You attempted to read an unlimited feature value");
		return limit;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("FeatureValue(");

		if (isUnlimited()) {
			sb.append("unlimited");
		} else {
			sb.append("limit=").append(limit);
		}

		return sb.append(")").toString();
	}
}
