package com.konfigyr.artifactory;

import com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static org.assertj.core.api.Assertions.*;

class ArtifactoryTest extends AbstractIntegrationTest {

	@Autowired
	Artifactory artifactory;

	@Autowired
	MetadataStore store;

	@Autowired
	DSLContext context;

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
				.satisfies(it -> assertThat(it.publishedAt())
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
	void shouldPublishArtifact(AssertablePublishedEvents events) {
		final var artifact = TestArtifacts.artifact(builder -> builder.version("3.0.0"));
		final var metadata = TestArtifacts.metadata(artifact);

		assertThat(artifactory.publish(Owners.konfigyr(), metadata))
				.isNotNull()
				.returns(artifact.groupId(), VersionedArtifact::groupId)
				.returns(artifact.artifactId(), VersionedArtifact::artifactId)
				.returns(artifact.version(), VersionedArtifact::version)
				.returns(artifact.name(), VersionedArtifact::name)
				.returns(artifact.description(), VersionedArtifact::description)
				.returns(artifact.website(), VersionedArtifact::website)
				.returns(artifact.repository(), VersionedArtifact::repository)
				.returns(PublicationState.PENDING, VersionedArtifact::state)
				.returns("8d9d53cfd5d27febf82baf0f8d801545358c1cf21e3d54cf9c2e5c5ba1754b98", VersionedArtifact::checksum)
				.satisfies(it -> assertThat(it.publishedAt())
						.isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
				);

		assertThat(store.get(ArtifactCoordinates.of(artifact)))
				.as("Should store property descriptor in the metadata store")
				.isPresent();

		events.assertThat()
				.as("Should publish an event for the new artifact release")
				.contains(ArtifactoryEvent.PublicationCreated.class)
				.matching(ArtifactoryEvent.PublicationCreated::coordinates, ArtifactCoordinates.of(artifact));
	}

	@Test
	@DisplayName("should fail to create a new artifact release when version already exists")
	void shouldPublishExistingArtifact(AssertablePublishedEvents events) {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:1.0.0");
		final var metadata = TestArtifacts.metadata(coordinates);

		assertThatExceptionOfType(ArtifactVersionExistsException.class)
				.isThrownBy(() -> artifactory.publish(Owners.konfigyr(), metadata))
				.withMessageContaining("Artifact version already exists for following coordinates: %s", coordinates)
				.returns(coordinates, ArtifactVersionExistsException::getCoordinates)
				.withNoCause();

		assertThat(store.get(coordinates))
				.as("Should not store property descriptor in the metadata store")
				.isEmpty();

		assertThat(events.ofType(ArtifactoryEvent.PublicationCreated.class))
				.as("Should not publish any release event when artifact release fails")
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to release an artifact when the owner has no active claim covering the groupId")
	void shouldRejectPublishForUnverifiedGroupId(AssertablePublishedEvents events) {
		final var coordinates = ArtifactCoordinates.parse("com.unverified:some-artifact:1.0.0");
		final var metadata = TestArtifacts.metadata(coordinates);

		assertThatExceptionOfType(GroupIdNotVerifiedException.class)
				.isThrownBy(() -> artifactory.publish(Owners.konfigyr(), metadata))
				.returns("com.unverified", GroupIdNotVerifiedException::getGroupId)
				.returns("konfigyr", ex -> ex.getOwner().slug())
				.returns(HttpStatus.BAD_REQUEST, GroupIdNotVerifiedException::getStatusCode);

		assertThat(store.get(coordinates))
				.as("Should not store property descriptor when the groupId is not verified")
				.isEmpty();

		assertThat(events.ofType(ArtifactoryEvent.PublicationCreated.class))
				.as("Should not publish any release event when ownership is not verified")
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
	@DisplayName("should resolve the subset of coordinates that already exist in the Artifactory")
	void shouldResolveExistingCoordinates() {
		final var existing = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");
		final var alsoExisting = ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:1.0.0");
		final var unknownVersion = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:9.0.0");
		final var unknownArtifact = ArtifactCoordinates.parse("com.konfigyr:konfigyr-unknown:1.0.0");

		assertThat(artifactory.existing(Owners.konfigyr(), List.of(existing, alsoExisting, unknownVersion, unknownArtifact)))
				.extracting(ArtifactCoordinates::of)
				.containsExactlyInAnyOrder(existing, alsoExisting);
	}

	@Test
	@DisplayName("should resolve an empty set of existing coordinates when none of them exist")
	void shouldResolveNoExistingCoordinates() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-unknown:1.0.0");

		assertThat(artifactory.existing(Owners.konfigyr(), List.of(coordinates)))
				.isEmpty();
	}

	@Test
	@DisplayName("should resolve an empty set of existing coordinates without querying when given an empty collection")
	void shouldResolveExistingCoordinatesForEmptyCollection() {
		assertThat(artifactory.existing(Owners.konfigyr(), List.of()))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve a public versioned artifact for any owner")
	void shouldRetrieveVisiblePublicVersionedArtifact() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		assertThat(artifactory.get(Owners.johnDoe(), coordinates))
				.isPresent()
				.get(InstanceOfAssertFactories.type(VersionedArtifact.class))
				.returns(ArtifactVisibility.PUBLIC, VersionedArtifact::visibility);

		assertThat(artifactory.get(null, coordinates))
				.as("a caller with no namespace context should still see a public artifact")
				.isPresent()
				.get(InstanceOfAssertFactories.type(VersionedArtifact.class))
				.returns(ArtifactVisibility.PUBLIC, VersionedArtifact::visibility);
	}

	@Test
	@DisplayName("should fail to retrieve a versioned artifact for any owner when no such version exists")
	void shouldRetrieveNoVisibleVersionedArtifactWhenUnknown() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:99.0.0");

		assertThat(artifactory.get(Owners.konfigyr(), coordinates))
				.isEmpty();
	}

	@Test
	@DisplayName("should only retrieve a private versioned artifact for its owning namespace")
	void shouldRetrieveVisiblePrivateVersionedArtifactOnlyForOwner() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-internal-secrets:1.0.0");

		assertThat(artifactory.get(Owners.konfigyr(), coordinates))
				.as("the owning namespace should see its own private artifact")
				.isPresent()
				.get(InstanceOfAssertFactories.type(VersionedArtifact.class))
				.returns(ArtifactVisibility.PRIVATE, VersionedArtifact::visibility);

		assertThat(artifactory.get(Owners.johnDoe(), coordinates))
				.as("a different namespace should not see a private artifact it doesn't own")
				.isEmpty();

		assertThat(artifactory.get(null, coordinates))
				.as("a caller with no namespace context should not see a private artifact")
				.isEmpty();
	}

	@Test
	@DisplayName("should determine existence of a public artifact version for any owner")
	void shouldDetermineExistenceOfVisiblePublicVersionedArtifact() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		assertThat(artifactory.exists(Owners.johnDoe(), coordinates)).isTrue();

		assertThat(artifactory.exists(null, coordinates))
				.as("a caller with no namespace context should still see a public artifact exists")
				.isTrue();
	}

	@Test
	@DisplayName("should determine non-existence of an unknown artifact version for any owner")
	void shouldDetermineNoExistenceOfVisibleVersionedArtifactWhenUnknown() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:99.0.0");

		assertThat(artifactory.exists(Owners.konfigyr(), coordinates)).isFalse();
	}

	@Test
	@DisplayName("should only determine existence of a private artifact version for its owning namespace")
	void shouldDetermineExistenceOfVisiblePrivateVersionedArtifactOnlyForOwner() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-internal-secrets:1.0.0");

		assertThat(artifactory.exists(Owners.konfigyr(), coordinates))
				.as("the owning namespace should see its own private artifact exists")
				.isTrue();

		assertThat(artifactory.exists(Owners.johnDoe(), coordinates))
				.as("a different namespace should not see a private artifact it doesn't own")
				.isFalse();

		assertThat(artifactory.exists(null, coordinates))
				.as("a caller with no namespace context should not see a private artifact exists")
				.isFalse();
	}

	@Test
	@DisplayName("should reject publishing a new version when the existing artifact is owned by a different namespace")
	void shouldRejectPublishWhenExistingArtifactOwnedByDifferentNamespace() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:reclaimed-artifact:1.0.0");
		final var metadata = TestArtifacts.metadata(coordinates);

		assertThatExceptionOfType(ArtifactOwnershipMismatchException.class)
				.isThrownBy(() -> artifactory.publish(Owners.konfigyr(), metadata))
				.returns("com.konfigyr", ArtifactOwnershipMismatchException::getGroupId)
				.returns("reclaimed-artifact", ArtifactOwnershipMismatchException::getArtifactId)
				.returns(HttpStatus.CONFLICT, ArtifactOwnershipMismatchException::getStatusCode);

		assertThat(artifactory.exists(coordinates))
				.as("no new version should have been created for the rejected publish attempt")
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should change visibility of an owned artifact")
	void shouldChangeVisibilityForOwnedArtifact() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-internal-secrets:1.0.0");

		assertThat(artifactory.get(Owners.johnDoe(), coordinates))
				.as("should not yet be visible to a different namespace while still private")
				.isEmpty();

		artifactory.changeVisibility(Owners.konfigyr(), "com.konfigyr", "konfigyr-internal-secrets", ArtifactVisibility.PUBLIC);

		assertThat(artifactory.get(Owners.johnDoe(), coordinates))
				.as("should now be visible to a different namespace after becoming public")
				.isPresent()
				.get(InstanceOfAssertFactories.type(VersionedArtifact.class))
				.returns(ArtifactVisibility.PUBLIC, VersionedArtifact::visibility);
	}

	@Test
	@Transactional
	@DisplayName("should reject changing visibility when the caller does not own the artifact")
	void shouldRejectChangeVisibilityForDifferentOwner() {
		assertThatExceptionOfType(ArtifactOwnershipMismatchException.class)
				.isThrownBy(() -> artifactory.changeVisibility(Owners.johnDoe(), "com.konfigyr", "konfigyr-internal-secrets", ArtifactVisibility.PUBLIC))
				.returns("com.konfigyr", ArtifactOwnershipMismatchException::getGroupId)
				.returns("konfigyr-internal-secrets", ArtifactOwnershipMismatchException::getArtifactId)
				.returns(HttpStatus.CONFLICT, ArtifactOwnershipMismatchException::getStatusCode);

		assertThat(artifactory.get(Owners.konfigyr(), ArtifactCoordinates.parse("com.konfigyr:konfigyr-internal-secrets:1.0.0")))
				.as("visibility must remain unchanged after a rejected attempt")
				.isPresent()
				.get(InstanceOfAssertFactories.type(VersionedArtifact.class))
				.returns(ArtifactVisibility.PRIVATE, VersionedArtifact::visibility);
	}

	@Test
	@DisplayName("should reject changing visibility for an unknown artifact")
	void shouldRejectChangeVisibilityForUnknownArtifact() {
		assertThatExceptionOfType(ArtifactDefinitionNotFoundException.class)
				.isThrownBy(() -> artifactory.changeVisibility(Owners.konfigyr(), "com.konfigyr", "konfigyr-does-not-exist", ArtifactVisibility.PUBLIC))
				.returns("com.konfigyr", ArtifactDefinitionNotFoundException::getGroupId)
				.returns("konfigyr-does-not-exist", ArtifactDefinitionNotFoundException::getArtifactId)
				.returns(HttpStatus.NOT_FOUND, ArtifactDefinitionNotFoundException::getStatusCode);
	}

}
