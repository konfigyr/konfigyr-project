package com.konfigyr.feature;

import org.jspecify.annotations.NonNull;

/**
 * Provider interface used to resolve or extract {@link FeatureValues} that are assigned to
 * {@link com.konfigyr.namespace.Namespace Namespaces}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@FunctionalInterface
public interface FeatureValuesProvider {

	/**
	 * Retrieves the {@link FeatureValues} that are granted to the a {@link com.konfigyr.namespace.Namespace}
	 * with a matching slug.
	 * <p>
	 * This method should not throw any exceptions or return {@code null} in case there is no matching
	 * {@link com.konfigyr.namespace.Namespace}, instead an empty feature values should be returned.
	 *
	 * @param namespace namespace slug for which values should be provided, can't be {@literal null}.
	 * @return assigned feature values, never {@literal null}.
	 */
	@NonNull
	FeatureValues get(@NonNull String namespace);

}
