package com.konfigyr.feature;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring autoconfiguration class for <code>konfigyr-features</code> module.
 *
 * @author Vladimir Spasic
 **/
@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class FeaturesAutoConfiguration {

	@Bean
	FeatureDefinitions featuresDefinitions(ObjectProvider<FeatureDefinitionConfigurer> configurers) {
		final List<FeatureDefinition<?>> definitions = new ArrayList<>();
		configurers.forEach(configurer -> configurer.configure(definitions));
		return new FeatureDefinitions(definitions);
	}

	@Bean
	FeatureValuesProvider betaFeatureValuesProvider() {
		return new BetaFeatureValuesProvider();
	}

	@Bean
	Features features(FeatureDefinitions definitions, ObjectProvider<FeatureValuesProvider> providers) {
		return new ProviderFeatures(definitions, providers);
	}

}
