package com.konfigyr.artifactory.provenance;

import com.konfigyr.artifactory.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;

@Slf4j
@RequiredArgsConstructor
class ProvenanceProcessor implements ItemProcessor<@NonNull PropertyDescriptor, @NonNull EvaluationResult> {

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
	public EvaluationResult process(@NonNull PropertyDescriptor property) {
		final EvaluationResult result = evaluator.evaluate(version, property);

		if (result instanceof EvaluationResult.Used) {
			log.debug("Provenance evaluation result detected that PropertyDescriptor({}) is already used by version: {}.",
					property.name(), version.coordinates().format());

			return null;
		}

		return result;
	}

	static VersionedArtifact lookup(ArtifactCoordinates coordinates, Artifactory artifactory) {
		return artifactory.get(coordinates).orElseThrow(() -> new ArtifactVersionNotFoundException(coordinates));
	}
}
