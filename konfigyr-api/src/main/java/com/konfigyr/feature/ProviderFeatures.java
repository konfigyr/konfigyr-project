package com.konfigyr.feature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.StreamUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
class ProviderFeatures implements Features {

	private final FeatureDefinitions definitions;
	private final Iterable<FeatureValuesProvider> providers;

	@NonNull
	@Override
	public <T extends FeatureValue> Optional<T> get(@NonNull String namespace, @NonNull FeatureDefinition<T> definition) {
		Assert.isTrue(definitions.has(definition), () -> "You attempted to obtain a feature value for feature definition that is not registered.");

		final FeatureValues values = StreamUtils.createStreamFromIterator(providers.iterator())
				.map(provider -> provider.get(namespace))
				.reduce(FeatureValues::concat)
				.orElseGet(FeatureValues::empty);

		if (log.isDebugEnabled()) {
			log.debug("Obtained following feature values for Namespace({}): {}", namespace, values);
		}

		return values.get(definition);
	}

}
