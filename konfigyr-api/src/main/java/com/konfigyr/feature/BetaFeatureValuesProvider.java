package com.konfigyr.feature;

import org.jspecify.annotations.NonNull;

/**
 * Feature values provider that provides Konfigyr Beta available features.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
final class BetaFeatureValuesProvider implements FeatureValuesProvider {

	private static final FeatureValues BETA_FEATURES = FeatureValues.builder()
			.add("namespace.members_count", FeatureValue.unlimited())
			.build();

	@NonNull
	@Override
	public FeatureValues get(@NonNull String namespace) {
		return BETA_FEATURES;
	}
}
