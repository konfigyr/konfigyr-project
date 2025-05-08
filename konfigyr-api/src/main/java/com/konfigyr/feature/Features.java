package com.konfigyr.feature;

import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.util.Optional;

/**
 * Interface that defines how to read and access {@link FeatureValue feature values} that are assigned to a
 * {@link com.konfigyr.namespace.Namespace}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface Features extends Serializable {

	/**
	 * Retrieve the {@link FeatureValue} for the specified {@link FeatureDefinition} that was assigned to
	 * a {@link com.konfigyr.namespace.Namespace} identified by its slug.
	 *
	 * @param namespace namespace slug, can't be {@literal null}.
	 * @param definition feature definition for which the value is retrieved, can't be {@literal null}.
	 * @param <T> feature value type, defined by the feature definition.
	 * @return {@link Optional} containing the assigned feature value or empty when not assigned or
	 * when the given {@link com.konfigyr.namespace.Namespace} is unknown.
	 */
	@NonNull
	<T extends FeatureValue> Optional<T> get(@NonNull String namespace, @NonNull FeatureDefinition<T> definition);

	/**
	 * Checks if the specified {@link FeatureDefinition} was assigned to a {@link com.konfigyr.namespace.Namespace}
	 * identified by its slug.
	 *
	 * @param namespace namespace slug, can't be {@literal null}.
	 * @param definition feature definition for which the value is checked, can't be {@literal null}.
	 * @return {@code true} when the feature value is assigned, {@code false} otherwise.
	 */
	default boolean has(@NonNull String namespace, @NonNull FeatureDefinition<? extends FeatureValue> definition) {
		return get(namespace, definition).isPresent();
	}

}
