package com.konfigyr.artifactory.provenance;

import com.konfigyr.artifactory.PropertyMetadata;
import com.konfigyr.artifactory.VersionedArtifact;
import org.jspecify.annotations.NonNull;

/**
 * Interface responsible for evaluating and generating the {@link Provenance} records based on incoming
 * {@link PropertyMetadata property metadata}. This evaluator is designed to handle property data that arrives
 * out of chronological order.
 * <p>
 * The core functionality revolves around comparing the {@link VersionedArtifact artifact versions} to determine
 * the earliest and latest versions a property has been observed in, as well as tracking the number of distinct
 * versions that use this property.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface ProvenanceEvaluator {

	/**
	 * Evaluates the {@link Provenance} for the {@link PropertyMetadata} that was released under the
	 * given {@link VersionedArtifact artifact version}.
	 *
	 * @param version the artifact version that declared the property metadata, can't be {@literal null}.
	 * @param metadata the Spring Boot configuration property metadata, can't be {@literal null}.
	 * @return the evaluated provenance result for the Spring Boot configuration property metadata, never {@literal null}.
	 */
	@NonNull
	EvaluationResult evaluate(@NonNull VersionedArtifact version, @NonNull PropertyMetadata metadata);

}
