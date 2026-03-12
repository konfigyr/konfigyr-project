package com.konfigyr.artifactory.provenance;

import com.konfigyr.artifactory.*;
import com.konfigyr.io.ByteArray;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.version.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProvenanceEvaluatorTest extends AbstractIntegrationTest {

	@Autowired
	Artifactory artifactory;

	@Autowired
	ProvenanceEvaluator evaluator;

	@Test
	@DisplayName("should perform provenance evaluation on new property metadata for version")
	void newPropertyMetadata() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");
		final var version = artifactory.get(coordinates).orElseThrow();

		final var metadata = metadata(
				"spring.banner.charset",
				"java.nio.charset.Charset",
				"The charset to use for the banner. Defaults to UTF-8.",
				StringSchema.builder().format("charset").build()
		);

		assertThat(evaluator.evaluate(version, metadata))
				.isNotNull()
				.isInstanceOf(EvaluationResult.New.class)
				.returns(version, EvaluationResult::artifact)
				.returns(metadata, EvaluationResult::property)
				.extracting(EvaluationResult::provenance)
				.returns(coordinates.version(), Provenance::firstSeen)
				.returns(coordinates.version(), Provenance::lastSeen)
				.returns(1, Provenance::occurrences)
				.returns(ByteArray.fromBase64String("j6s_LeZ1xec9fMrK6-du04pJzEeossisWzNVYl6cKBY="), Provenance::checksum);
	}

	@Test
	@DisplayName("should perform provenance evaluation on property metadata that should be linked to the first seen version")
	void unusedPropertyMetadataWithFirstSeenVersion() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");
		final var version = artifactory.get(coordinates).orElseThrow();

		final var metadata = metadata(
				"spring.application.name",
				"java.lang.String",
				"Application name. Typically used with logging to help identify the application.",
				StringSchema.instance()
		);

		assertThat(evaluator.evaluate(version, metadata))
				.isNotNull()
				.isInstanceOf(EvaluationResult.Unused.class)
				.returns(version, EvaluationResult::artifact)
				.returns(metadata, EvaluationResult::property)
				.extracting(EvaluationResult::provenance)
				.returns(coordinates.version(), Provenance::firstSeen)
				.returns(Version.of("1.0.1"), Provenance::lastSeen)
				.returns(2, Provenance::occurrences)
				.returns(ByteArray.fromBase64String("cRJ8jlPpTPmTJEoZEZNDSjvdqafG05QkzNJplXyu9J0="), Provenance::checksum);
	}

	@Test
	@DisplayName("should perform provenance evaluation on property metadata that should be linked to the last seen version")
	void unusedPropertyMetadataWithLastSeenVersion() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.2");
		final var version = artifactory.get(coordinates).orElseThrow();

		final var metadata = metadata(
				"spring.application.name",
				"java.lang.String",
				"Application name. Typically used with logging to help identify the application.",
				StringSchema.instance()
		);

		assertThat(evaluator.evaluate(version, metadata))
				.isNotNull()
				.isInstanceOf(EvaluationResult.Unused.class)
				.returns(version, EvaluationResult::artifact)
				.returns(metadata, EvaluationResult::property)
				.extracting(EvaluationResult::provenance)
				.returns(Version.of("1.0.1"), Provenance::firstSeen)
				.returns(coordinates.version(), Provenance::lastSeen)
				.returns(2, Provenance::occurrences)
				.returns(ByteArray.fromBase64String("cRJ8jlPpTPmTJEoZEZNDSjvdqafG05QkzNJplXyu9J0="), Provenance::checksum);
	}

	@Test
	@DisplayName("should perform provenance evaluation on property metadata that is already linked to the version")
	void usedPropertyMetadata() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.1");
		final var version = artifactory.get(coordinates).orElseThrow();

		final var metadata = metadata(
				"spring.application.name",
				"java.lang.String",
				"Application name. Typically used with logging to help identify the application.",
				StringSchema.instance()
		);

		assertThat(evaluator.evaluate(version, metadata))
				.isNotNull()
				.isInstanceOf(EvaluationResult.Used.class)
				.returns(version, EvaluationResult::artifact)
				.returns(metadata, EvaluationResult::property)
				.extracting(EvaluationResult::provenance)
				.returns(coordinates.version(), Provenance::firstSeen)
				.returns(coordinates.version(), Provenance::lastSeen)
				.returns(1, Provenance::occurrences)
				.returns(ByteArray.fromBase64String("cRJ8jlPpTPmTJEoZEZNDSjvdqafG05QkzNJplXyu9J0="), Provenance::checksum);
	}

	static PropertyDescriptor metadata(String name, String typeName, String description, JsonSchema schema) {
		return PropertyDescriptor.builder()
				.name(name)
				.typeName(typeName)
				.schema(schema)
				.description(description)
				.build();
	}

}
