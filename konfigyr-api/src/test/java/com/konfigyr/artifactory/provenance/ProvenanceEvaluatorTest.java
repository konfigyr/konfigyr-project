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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ProvenanceEvaluatorTest extends AbstractIntegrationTest {

	@Autowired
	Artifactory artifactory;

	@Autowired
	ProvenanceEvaluator evaluator;

	@Test
	@DisplayName("should perform provenance evaluation on new property metadata for version")
	void newPropertyMetadata() {
		final var version = artifactory.get(ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0"))
				.orElseThrow();

		final var metadata = metadata(
				"spring.banner.charset",
				PropertyType.CHARSET,
				DataType.COLLECTION,
				"java.nio.charset.Charset",
				"The charset to use for the banner. Defaults to UTF-8."
		);

		assertThat(evaluator.evaluate(version, metadata))
				.isNotNull()
				.isInstanceOf(EvaluationResult.New.class)
				.returns(version, EvaluationResult::version)
				.returns(metadata, EvaluationResult::metadata)
				.extracting(EvaluationResult::provenance)
				.returns(version.version(), Provenance::firstSeen)
				.returns(version.version(), Provenance::lastSeen)
				.returns(1, Provenance::occurrences)
				.returns(ByteArray.fromBase64String("6-gplbdPGj3WwzUt7fWeOBCSijvuAtqR5pWwCkKFl8U="), Provenance::checksum);
	}

	@Test
	@DisplayName("should perform provenance evaluation on property metadata that should be linked to the first seen version")
	void unusedPropertyMetadataWithFirstSeenVersion() {
		final var version = artifactory.get(ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0"))
				.orElseThrow();

		final var metadata = metadata("spring.application.name", PropertyType.STRING, DataType.ATOMIC,
				"java.lang.String", "Application name. Typically used with logging to help identify the application.");

		assertThat(evaluator.evaluate(version, metadata))
				.isNotNull()
				.isInstanceOf(EvaluationResult.Unused.class)
				.returns(version, EvaluationResult::version)
				.returns(metadata, EvaluationResult::metadata)
				.extracting(EvaluationResult::provenance)
				.returns(version.version(), Provenance::firstSeen)
				.returns(Version.of("1.0.1"), Provenance::lastSeen)
				.returns(2, Provenance::occurrences)
				.returns(ByteArray.fromBase64String("8IiKOly5JR3uQJoeTBFU7BRkX7enEjgG-XwqPEv3lAo="), Provenance::checksum);
	}

	@Test
	@DisplayName("should perform provenance evaluation on property metadata that should be linked to the last seen version")
	void unusedPropertyMetadataWithLastSeenVersion() {
		final var version = artifactory.get(ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.2"))
				.orElseThrow();

		final var metadata = metadata("spring.application.name", PropertyType.STRING, DataType.ATOMIC,
				"java.lang.String", "Application name. Typically used with logging to help identify the application.");

		assertThat(evaluator.evaluate(version, metadata))
				.isNotNull()
				.isInstanceOf(EvaluationResult.Unused.class)
				.returns(version, EvaluationResult::version)
				.returns(metadata, EvaluationResult::metadata)
				.extracting(EvaluationResult::provenance)
				.returns(Version.of("1.0.1"), Provenance::firstSeen)
				.returns(version.version(), Provenance::lastSeen)
				.returns(2, Provenance::occurrences)
				.returns(ByteArray.fromBase64String("8IiKOly5JR3uQJoeTBFU7BRkX7enEjgG-XwqPEv3lAo="), Provenance::checksum);
	}

	@Test
	@DisplayName("should perform provenance evaluation on property metadata that is already linked to the version")
	void usedPropertyMetadata() {
		final var version = artifactory.get(ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.1"))
				.orElseThrow();

		final var metadata = metadata("spring.application.name", PropertyType.STRING, DataType.ATOMIC,
				"java.lang.String", "Application name. Typically used with logging to help identify the application.");

		assertThat(evaluator.evaluate(version, metadata))
				.isNotNull()
				.isInstanceOf(EvaluationResult.Used.class)
				.returns(version, EvaluationResult::version)
				.returns(metadata, EvaluationResult::metadata)
				.extracting(EvaluationResult::provenance)
				.returns(version.version(), Provenance::firstSeen)
				.returns(version.version(), Provenance::lastSeen)
				.returns(1, Provenance::occurrences)
				.returns(ByteArray.fromBase64String("8IiKOly5JR3uQJoeTBFU7BRkX7enEjgG-XwqPEv3lAo="), Provenance::checksum);
	}

	static PropertyMetadata metadata(String name, PropertyType type, DataType dataType, String typeName, String description) {
		final var metadata = mock(PropertyMetadata.class);
		doReturn(name).when(metadata).name();
		doReturn(type).when(metadata).type();
		doReturn(dataType).when(metadata).dataType();
		doReturn(typeName).when(metadata).typeName();
		doReturn(description).when(metadata).description();
		return metadata;
	}

}
