package com.konfigyr.feature;

import org.springframework.lang.NonNull;

import java.io.Serializable;

/**
 * Interface that defines the subscription plan feature used within the Konfigyr application.
 * <p>
 * You can register your {@link FeatureDefinition feature definitions} via the {@link FeatureDefinitionConfigurer}
 * and access them via the {@link FeatureDefinitions} Spring Bean.
 * <p>
 * To access the {@link FeatureValue feature values} that are assigned to a {@link com.konfigyr.namespace.Namespace},
 * you should use the {@link Features} Spring Bean.
 *
 * @param <T> feature value type
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Features
 * @see FeatureDefinitions
 */
public interface FeatureDefinition<T extends FeatureValue> extends Comparable<FeatureDefinition<?>>, Serializable {

	/**
	 * Creates a new {@link FeatureDefinition} with a name, group and value type.
	 *
	 * @param name feature name, can't be blank.
	 * @param type feature value type, can't be {@literal null}.
	 * @param <T> the generic feature value type
	 * @return the feature definition, never {@literal null}.
	 */
	@NonNull
	static <T extends FeatureValue> FeatureDefinition<T> of(String name, Class<T> type) {
		return new SimpleFeatureDefinition<>(name, type);
	}

	/**
	 * Name of the feature that should be unique across the application. Features names should use
	 * only alphanumeric characters.
	 *
	 * @return the feature name, can't be {@literal null}.
	 */
	@NonNull
	String name();

	/**
	 * The actual type of the {@link FeatureValue} that this feature uses.
	 *
	 * @return the feature value type, can't be {@literal null}.
	 */
	@NonNull
	Class<T> type();

}
