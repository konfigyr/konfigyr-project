package com.konfigyr.artifactory.provenance;

import com.konfigyr.artifactory.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@Slf4j
@RequiredArgsConstructor
class ProvenanceProcessor implements ItemProcessor<PropertyMetadata, EvaluationResult> {

	private final VersionedArtifact version;
	private final ProvenanceEvaluator evaluator;

	ProvenanceProcessor(String coordinates, Artifactory artifactory, ProvenanceEvaluator evaluator) {
		this(ArtifactCoordinates.parse(coordinates), artifactory, evaluator);
	}

	ProvenanceProcessor(ArtifactCoordinates coordinates, Artifactory artifactory, ProvenanceEvaluator evaluator) {
		this(lookup(coordinates, artifactory), evaluator);
	}

	@Nullable
	@Override
	public EvaluationResult process(@NonNull PropertyMetadata metadata) {
		final EvaluationResult result = evaluator.evaluate(version, metadata);

		if (result instanceof EvaluationResult.Used) {
			log.debug("Provenance evaluation result detected that PropertyMetadata({}) is already used by version: {}.",
					metadata.name(), version.format());

			return null;
		}

		return result;
	}

	static VersionedArtifact lookup(ArtifactCoordinates coordinates, Artifactory artifactory) {
		return artifactory.get(coordinates).orElseThrow(() -> new ArtifactVersionNotFoundException(coordinates));
	}
}
