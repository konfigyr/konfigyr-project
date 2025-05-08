package com.konfigyr.feature;

@SuppressWarnings("InterfaceIsType")
public interface TestFeatureDefinitions {

	FeatureDefinition<LimitedFeatureValue> LIMITED = FeatureDefinition.of("limited", LimitedFeatureValue.class);

	FeatureDefinition<RateLimitFeatureValue> RATE_LIMITED = FeatureDefinition.of("rate-limited", RateLimitFeatureValue.class);

}
