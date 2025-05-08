package com.konfigyr.feature;

import org.springframework.lang.NonNull;

import java.util.Collection;

/**
 * Configurer interface that is used to add and register {@link FeatureDefinition feature definitions}
 * via Spring Boot configuration classes.
 * <p>
 * Registered definitions can later be accessed via {@link FeatureDefinitions} Spring Bean.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@FunctionalInterface
public interface FeatureDefinitionConfigurer {

	/**
	 * Register {@link FeatureDefinition feature definitions} in the supplied collection.
	 *
	 * @param definitions collection of feature definitions, can't be {@literal null}.
	 */
	void configure(@NonNull Collection<FeatureDefinition<?>> definitions);

}
