package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.security.util.InMemoryResource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ArtifactoryTest extends AbstractIntegrationTest {

	@Autowired
	Artifactory artifactory;

	@Test
	@DisplayName("should retrieve versioned artifact by coordinates")
	void shouldRetrieveVersionedArtifact() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		assertThat(artifactory.get(coordinates))
				.isPresent()
				.get(InstanceOfAssertFactories.type(VersionedArtifact.class))
				.returns(EntityId.from(2), VersionedArtifact::id)
				.returns(coordinates.groupId(), VersionedArtifact::groupId)
				.returns(coordinates.artifactId(), VersionedArtifact::artifactId)
				.returns(coordinates.version(), VersionedArtifact::version)
				.returns("Konfigyr Crypto API", VersionedArtifact::name)
				.returns("Spring Boot Crypto API library", VersionedArtifact::description)
				.returns(null, VersionedArtifact::checksum)
				.returns(null, VersionedArtifact::website)
				.returns(URI.create("https://github.com/konfigyr/konfigyr-crypto"), VersionedArtifact::repository)
				.satisfies(it -> assertThat(it.releasedAt())
						.isCloseTo(OffsetDateTime.now().minusDays(7), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should fail to retrieve versioned artifact by coordinates when no such version exists")
	void shouldRetrieveUnknownArtifactVersion() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:99.0.0");

		assertThat(artifactory.get(coordinates))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to retrieve versioned artifact by coordinates when no such artifact exists")
	void shouldRetrieveUnknownArtifact() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-unknown:1.0.0");

		assertThat(artifactory.get(coordinates))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should create a new artifact release")
	void shouldReleaseArtifact(AssertablePublishedEvents events) {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-identity:2.0.0");
		final var resource = new InMemoryResource("spring-boot-property-metadata.json");

		assertThatNoException().isThrownBy(() -> artifactory.release(coordinates, resource));

		events.assertThat()
				.contains(ArtifactoryEvent.Release.class)
				.matching(ArtifactoryEvent.Release::coordinates, coordinates);
	}

	@Test
	@DisplayName("should fail to create a new artifact release when metadata resource can not be saved")
	void shouldReleaseCorruptArtifactMetadata(AssertablePublishedEvents events) {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-identity:2.0.0");
		final var resource = new InputStreamResource(() -> {
			throw new IOException("Corrupt metadata file");
		});

		assertThatExceptionOfType(ArtifactoryException.class)
				.isThrownBy(() -> artifactory.release(coordinates, resource))
				.withMessageContaining("Unexpected error occurred while storing metadata for artifact: %s", coordinates)
				.havingRootCause()
				.isInstanceOf(IOException.class)
				.withMessage("Corrupt metadata file");

		assertThat(events.ofType(ArtifactoryEvent.Release.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to create a new artifact release when version already exists")
	void shouldReleaseExistingArtifact(AssertablePublishedEvents events) {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:1.0.0");
		final var resource = new InMemoryResource("spring-boot-property-metadata.json");

		assertThatExceptionOfType(ArtifactVersionExistsException.class)
				.isThrownBy(() -> artifactory.release(coordinates, resource))
				.withMessageContaining("Artifact version already exists for following coordinates: %s", coordinates)
				.returns(coordinates, ArtifactVersionExistsException::getCoordinates)
				.withNoCause();

		assertThat(events.ofType(ArtifactoryEvent.Release.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve properties for an artifact version")
	void retrieveArtifactProperties() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.1");

		assertThat(artifactory.properties(coordinates))
				.hasSize(1)
				.first()
				.returns(EntityId.from(1), PropertyDefinition::id)
				.returns(EntityId.from(2), PropertyDefinition::artifact)
				.returns(ByteArray.fromBase64String("8IiKOly5JR3uQJoeTBFU7BRkX7enEjgG-XwqPEv3lAo="), PropertyDefinition::checksum)
				.returns(PropertyType.STRING, PropertyDefinition::type)
				.returns(DataType.ATOMIC, PropertyDefinition::dataType)
				.returns("java.lang.String", PropertyDefinition::typeName)
				.returns("spring.application.name", PropertyDefinition::name)
				.returns("Application name. Typically used with logging to help identify the application.", PropertyDefinition::description)
				.returns(null, PropertyDefinition::defaultValue)
				.returns(null, PropertyDefinition::deprecation)
				.returns(List.of(), PropertyDefinition::hints)
				.returns(1, PropertyDefinition::occurrences)
				.returns(coordinates.version(), PropertyDefinition::firstSeen)
				.returns(coordinates.version(), PropertyDefinition::lastSeen);
	}

	@Test
	@DisplayName("should retrieve properties for an artifact version that has no properties")
	void retrieveEmptyArtifactProperties() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		assertThat(artifactory.properties(coordinates))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to retrieve properties for an unknown artifact version")
	void retrieveUnknownArtifactProperties() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:9.0.0");

		assertThatExceptionOfType(ArtifactVersionNotFoundException.class)
				.isThrownBy(() -> artifactory.properties(coordinates))
				.withMessageContaining("Can not find artifact version with following coordinates: %s", coordinates)
				.returns(coordinates, ArtifactVersionNotFoundException::getCoordinates)
				.returns(HttpStatus.NOT_FOUND, ArtifactVersionNotFoundException::getStatusCode)
				.withNoCause();
	}

}
