package com.konfigyr.artifactory.provenance;

import com.konfigyr.artifactory.PropertyMetadata;
import com.konfigyr.artifactory.VersionedArtifact;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * Interface that represents the result of a {@link com.konfigyr.artifactory.PropertyMetadata} provenance evaluation
 * for a specific {@link VersionedArtifact artifact version}.
 * <p>
 * This interface is intentionally sealed to ensure that we cover all possible outcomes and to provide a type-safe
 * way to manage state transitions of {@link com.konfigyr.artifactory.PropertyMetadata} in the Artifactory domain.
 * <p>
 * This is a list of the following evaluation outcomes:
 * <ul>
 *     <li>
 *         <strong>New property</strong>: the property that is being evaluated is not yet known and should be
 *         inserted in the Artifactory with the initial {@link Provenance} state.
 *     </li>
 *     <li>
 *         <strong>Unused property</strong>: the evaluated property is already known to the Artifactory, but it's
 *         not usage is not yet linked to the affected artifact version. The result would contain an updated
 *         {@link Provenance} record with new occurrences state and potentially the first and last seen versions.
 *     </li>
 *     <li>
 *         <strong>Used property</strong>: this means that the evaluated property is known, and it's usage is
 *         already linked to the affected artifact version in the Artifactory. The result would contain the
 *         current unmodified {@link Provenance} record state.
 *     </li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ProvenanceEvaluator
 */
public sealed interface EvaluationResult extends Serializable
		permits EvaluationResult.New, EvaluationResult.Unused, EvaluationResult.Used {

	/**
	 * The version of the {@link com.konfigyr.artifactory.Artifact} that owns the {@link PropertyMetadata} that
	 * was the subject of the evaluation.
	 *
	 * @return the versioned artifact, never {@literal null}.
	 */
	@NonNull
	VersionedArtifact version();

	/**
	 * The {@link PropertyMetadata} that was the subject of the evaluation.
	 *
	 * @return the property metadata, never {@literal null}.
	 */
	@NonNull
	PropertyMetadata metadata();

	/**
	 * The evaluated {@link Provenance} record of the {@link PropertyMetadata} and specified {@link VersionedArtifact}.
	 *
	 * @return the property metadata provenance record, never {@literal null}.
	 */
	@NonNull
	Provenance provenance();

	/**
	 * A record representing the evaluation result when a new property that should be inserted into the
	 * artifactory for the specified {@link VersionedArtifact artifact version}.
	 *
	 * @param version affected artifact version, can't be {@literal null}.
	 * @param metadata evaluated property metadata, can't be {@literal null}
	 * @param provenance The initial provenance record for the new property, can't be {@literal null}.
	 */
	record New(VersionedArtifact version, PropertyMetadata metadata, Provenance provenance) implements EvaluationResult {
		@Serial
		private static final long serialVersionUID = 5582579202930940434L;
	}

	/**
	 * A record representing the evaluation result when an existing property is currently not used by
	 * the specified {@link VersionedArtifact artifact version}. This result should force the
	 * artifactory to create a link between the property metadata and the artifact version.
	 *
	 * @param version affected artifact version, can't be {@literal null}
	 * @param metadata evaluated property metadata, can't be {@literal null}
	 * @param provenance The provenance record that should replace the old one, can't be {@literal null}.
	 */
	record Unused(VersionedArtifact version, PropertyMetadata metadata, Provenance provenance) implements EvaluationResult {
		@Serial
		private static final long serialVersionUID = 4853856795235686402L;
	}

	/**
	 * A record representing the evaluation result when an existing property that does not require any updates,
	 * as the new metadata doesn't change its provenance. This means that the property metadata is already
	 * linked to the given {@link VersionedArtifact artifact version} is ingested twice.
	 *
	 * @param version affected artifact version, can't be {@literal null}
	 * @param metadata evaluated property metadata, can't be {@literal null}
	 * @param provenance The provenance record that should stay the same, can't be {@literal null}.
	 */
	record Used(VersionedArtifact version, PropertyMetadata metadata, Provenance provenance) implements EvaluationResult {
		@Serial
		private static final long serialVersionUID = 8988138899110174470L;
	}

}
