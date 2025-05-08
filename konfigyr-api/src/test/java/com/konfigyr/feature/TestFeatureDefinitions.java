package com.konfigyr.feature;

public class TestFeatureDefinitions {

	static FeatureDefinition<LimitedFeatureValue> LIMITED = FeatureDefinition.of("limited", LimitedFeatureValue.class);

	static FeatureDefinition<RateLimitFeatureValue> RATE_LIMITED = FeatureDefinition.of("rate-limited", RateLimitFeatureValue.class);

}
