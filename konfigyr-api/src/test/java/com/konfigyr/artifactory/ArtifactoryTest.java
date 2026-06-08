package com.konfigyr.artifactory;

import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class ArtifactoryTest extends AbstractIntegrationTest {

	@Autowired
	Artifactory artifactory;

	@Autowired
	MetadataStore store;

	@Test
	@DisplayName("should retrieve versioned artifact by coordinates")
	void shouldRetrieveVersionedArtifact() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		assertThat(artifactory.get(coordinates))
				.isPresent()
				.get(InstanceOfAssertFactories.type(VersionedArtifact.class))
				.returns(EntityId.from(2), VersionedArtifact::id)
				.returns(coordinates, VersionedArtifact::coordinates)
				.returns(coordinates.groupId(), VersionedArtifact::groupId)
				.returns(coordinates.artifactId(), VersionedArtifact::artifactId)
				.returns(coordinates.version().get(), VersionedArtifact::version)
				.returns("Konfigyr Crypto API", VersionedArtifact::name)
				.returns("Spring Boot Crypto API library", VersionedArtifact::description)
				.returns("fdd0eabc2a212d35e21556f32bd10f82816ac7b2cc8ff3c7b5dff9c9ca5e57cb", VersionedArtifact::checksum)
				.returns(null, VersionedArtifact::website)
				.returns(URI.create("https://github.com/konfigyr/konfigyr-crypto"), VersionedArtifact::repository)
				.satisfies(it -> assertThat(it.releasedAt())
						.isCloseTo(
								OffsetDateTime.now().minusDays(7).toInstant(),
								within(1, ChronoUnit.HOURS)
						)
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
		final var artifact = TestArtifacts.artifact(builder -> builder.version("3.0.0"));
		final var metadata = TestArtifacts.metadata(artifact);

		assertThat(artifactory.release(metadata))
				.isNotNull()
				.returns(artifact.groupId(), VersionedArtifact::groupId)
				.returns(artifact.artifactId(), VersionedArtifact::artifactId)
				.returns(artifact.version(), VersionedArtifact::version)
				.returns(artifact.name(), VersionedArtifact::name)
				.returns(artifact.description(), VersionedArtifact::description)
				.returns(artifact.website(), VersionedArtifact::website)
				.returns(artifact.repository(), VersionedArtifact::repository)
				.returns(ReleaseState.PENDING, VersionedArtifact::state)
				.returns("8d9d53cfd5d27febf82baf0f8d801545358c1cf21e3d54cf9c2e5c5ba1754b98", VersionedArtifact::checksum)
				.satisfies(it -> assertThat(it.releasedAt())
						.isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
				);

		assertThat(store.get(ArtifactCoordinates.of(artifact)))
				.as("Should store property descriptor in the metadata store")
				.isPresent();

		events.assertThat()
				.as("Should publish an event for the new artifact release")
				.contains(ArtifactoryEvent.ReleaseCreated.class)
				.matching(ArtifactoryEvent.ReleaseCreated::coordinates, ArtifactCoordinates.of(artifact));
	}

	@Test
	@DisplayName("should fail to create a new artifact release when version already exists")
	void shouldReleaseExistingArtifact(AssertablePublishedEvents events) {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:1.0.0");
		final var metadata = TestArtifacts.metadata(coordinates);

		assertThatExceptionOfType(ArtifactVersionExistsException.class)
				.isThrownBy(() -> artifactory.release(metadata))
				.withMessageContaining("Artifact version already exists for following coordinates: %s", coordinates)
				.returns(coordinates, ArtifactVersionExistsException::getCoordinates)
				.withNoCause();

		assertThat(store.get(coordinates))
				.as("Should not store property descriptor in the metadata store")
				.isEmpty();

		assertThat(events.ofType(ArtifactoryEvent.ReleaseCreated.class))
				.as("Should not publish any release event when artifact release fails")
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
				.returns(ByteArray.fromBase64String("cRJ8jlPpTPmTJEoZEZNDSjvdqafG05QkzNJplXyu9J0="), PropertyDefinition::checksum)
				.returns("java.lang.String", PropertyDefinition::typeName)
				.returns(StringSchema.instance(), PropertyDefinition::schema)
				.returns("spring.application.name", PropertyDefinition::name)
				.returns("Application name. Typically used with logging to help identify the application.", PropertyDefinition::description)
				.returns(null, PropertyDefinition::defaultValue)
				.returns(null, PropertyDefinition::deprecation)
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

	@Test
	@DisplayName("should search all property definitions matching a term globally")
	void shouldSearchPropertiesByTermGlobally() {
		final var query = SearchQuery.builder()
				.term("spring.profiles")
				.pageable(Pageable.ofSize(20))
				.build();

		final var page = artifactory.search(query);

		assertThatObject(page)
				.returns(3L, Page::getTotalElements)
				.returns(1, Page::getTotalPages);

		assertThat(page)
				.map(PropertyDefinition::name)
				.containsExactly(
						"spring.profiles.active",
						"spring.profiles.default",
						"spring.profiles.validate"
				);
	}

	@Test
	@DisplayName("should search property definitions by description term globally")
	void shouldSearchPropertiesByDescriptionTermGlobally() {
		final var query = SearchQuery.builder()
				.term("Application name")
				.pageable(Pageable.ofSize(20))
				.build();

		assertThat(artifactory.search(query))
				.extracting(PropertyDefinition::name)
				.containsExactly("spring.application.name");
	}

	@Test
	@DisplayName("should return an empty page when no property definition matches the search term")
	void shouldReturnEmptyPageForUnmatchedSearchTerm() {
		final var query = SearchQuery.builder()
				.term("com.nonexistent.property.that.does.not.exist")
				.pageable(Pageable.ofSize(20))
				.build();

		assertThatObject(artifactory.search(query))
				.returns(0L, Page::getTotalElements)
				.returns(0, Page::getTotalPages)
				.returns(true, Page::isEmpty);
	}

	@Test
	@DisplayName("should search property definitions scoped to a specific artifact version")
	void shouldSearchPropertiesByArtifactCoordinates() {
		final var coordinates = ArtifactCoordinates.parse("org.springframework.boot:spring-boot:4.0.4");

		final var query = SearchQuery.builder()
				.criteria(PropertyDefinition.ARTIFACT_CRITERIA, coordinates)
				.pageable(Pageable.ofSize(20))
				.build();

		assertThat(artifactory.search(query))
				.map(PropertyDefinition::name)
				.containsExactly(
						"spring.profiles.active",
						"spring.profiles.default",
						"spring.profiles.validate"
				);
	}

	@Test
	@DisplayName("should combine artifact scope with a search term")
	void shouldSearchPropertiesByArtifactCoordinatesAndTerm() {
		final var coordinates = ArtifactCoordinates.parse("org.springframework.boot:spring-boot-autoconfigure:4.0.4");

		final var query = SearchQuery.builder()
				.term("messages")
				.criteria(PropertyDefinition.ARTIFACT_CRITERIA, coordinates)
				.pageable(Pageable.ofSize(20))
				.build();

		assertThat(artifactory.search(query))
				.map(PropertyDefinition::name)
				.containsExactly(
						"spring.messages.basename",
						"spring.messages.cache-duration",
						"spring.messages.encoding"
				);
	}

	@Test
	@DisplayName("should return an empty page when the artifact version has no properties")
	void shouldReturnEmptyPageWhenArtifactVersionHasNoProperties() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		final var query = SearchQuery.builder()
				.criteria(PropertyDefinition.ARTIFACT_CRITERIA, coordinates)
				.pageable(Pageable.ofSize(20))
				.build();

		assertThatObject(artifactory.search(query))
				.satisfies(page -> {
					assertThat(page.getTotalElements()).isZero();
					assertThat(page.getContent()).isEmpty();
				});
	}

	@Test
	@DisplayName("should return an empty page for unknown artifact coordinates")
	void shouldReturnEmptyPageForUnknownArtifactCoordinates() {
		final var coordinates = ArtifactCoordinates.parse("com.nonexistent:artifact:9.9.9");

		final var query = SearchQuery.builder()
				.criteria(PropertyDefinition.ARTIFACT_CRITERIA, coordinates)
				.pageable(Pageable.ofSize(20))
				.build();

		assertThatObject(artifactory.search(query))
				.returns(0L, Page::getTotalElements)
				.returns(0, Page::getTotalPages)
				.returns(true, Page::isEmpty);
	}

	@Test
	@DisplayName("should return paged results with correct total element count and page metadata")
	void shouldReturnPagedResultsWithCorrectMetadata() {
		final var query = SearchQuery.builder()
				.pageable(Pageable.ofSize(5))
				.build();

		assertThatObject(artifactory.search(query))
				.returns(5, Page::getNumberOfElements)
				.returns(16L, Page::getTotalElements)
				.returns(4, Page::getTotalPages)
				.returns(5, Page::getSize)
				.returns(0, Page::getNumber)
				.extracting(Page::getContent, InstanceOfAssertFactories.iterable(PropertyDefinition.class))
				.hasSize(5);
	}

	@Test
	@DisplayName("should exclude deprecated property definitions when INCLUDE_DEPRECATED_CRITERIA is false")
	void shouldExcludeDeprecatedPropertiesWhenRequested() {
		final var coordinates = ArtifactCoordinates.parse("org.springframework.boot:spring-boot-actuator:4.0.4");

		final var query = SearchQuery.builder()
				.criteria(PropertyDefinition.ARTIFACT_CRITERIA, coordinates)
				.criteria(PropertyDefinition.INCLUDE_DEPRECATED_CRITERIA, false)
				.pageable(Pageable.ofSize(20))
				.build();

		assertThat(artifactory.search(query))
				.extracting(PropertyDefinition::deprecation)
				.containsOnlyNulls();
	}

	@Test
	@DisplayName("should include all property definitions when INCLUDE_DEPRECATED_CRITERIA is true")
	void shouldIncludeDeprecatedPropertiesWhenRequested() {
		final var coordinates = ArtifactCoordinates.parse("org.springframework.boot:spring-boot-actuator:4.0.4");

		final var query = SearchQuery.builder()
				.criteria(PropertyDefinition.ARTIFACT_CRITERIA, coordinates)
				.criteria(PropertyDefinition.INCLUDE_DEPRECATED_CRITERIA, true)
				.pageable(Pageable.ofSize(20))
				.build();

		assertThat(artifactory.search(query))
				.map(PropertyDefinition::name)
				.containsExactly(
						"management.endpoint.sbom.access",
						"management.endpoint.sbom.application.media-type",
						"management.endpoint.shutdown.access",
						"management.endpoint.startup.access"
				);
	}

}
