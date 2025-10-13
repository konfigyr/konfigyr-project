package com.konfigyr.namespace;

import com.konfigyr.feature.FeatureDefinition;
import com.konfigyr.feature.LimitedFeatureValue;

/**
 * Interface that exposes Namespace {@link FeatureDefinition feature definitions}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@SuppressWarnings("InterfaceIsType")
public interface NamespaceFeatures {

	/**
	 * Feature definition that is used to define how many {@link Member members} can one {@link Namespace} have.
	 */
	FeatureDefinition<LimitedFeatureValue> MEMBERS_COUNT = FeatureDefinition.of("namespace.members_count", LimitedFeatureValue.class);

	/**
	 * Feature definition that is used to define how many {@link Service services} can one {@link Namespace} have.
	 */
	FeatureDefinition<LimitedFeatureValue> SERVICES_COUNT = FeatureDefinition.of("namespace.services_count", LimitedFeatureValue.class);

}
