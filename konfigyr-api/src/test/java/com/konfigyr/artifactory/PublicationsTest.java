package com.konfigyr.artifactory;

import com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class PublicationsTest extends AbstractIntegrationTest {

	@Autowired
	Publications publications;

	@Autowired
	MetadataStore store;

	@Test
	@DisplayName("should search artifacts owned by a namespace")
	void shouldSearchArtifactsOwnedByNamespace() {
		final var result = publications.artifacts(Owners.konfigyr(), SearchQuery.of(Pageable.ofSize(20)));

		assertThat(result.stream())
				.extracting(ArtifactDefinition::id)
				.containsExactlyInAnyOrder(
						EntityId.from(2), EntityId.from(3), EntityId.from(4), EntityId.from(5),
						EntityId.from(6), EntityId.from(7), EntityId.from(8), EntityId.from(9),
						EntityId.from(10), EntityId.from(11), EntityId.from(12), EntityId.from(14)
				);
	}

	@Test
	@DisplayName("should never include another namespace's artifacts in a search, even when public")
	void shouldNeverIncludeArtifactsOwnedByDifferentNamespaceInSearch() {
		final var result = publications.artifacts(Owners.johnDoe(), SearchQuery.of(Pageable.ofSize(20)));

		assertThat(result.stream())
				.as("john-doe owns only its own fixtures, never konfigyr's public artifacts")
				.extracting(ArtifactDefinition::id)
				.containsExactlyInAnyOrder(EntityId.from(1), EntityId.from(13), EntityId.from(15));
	}

	@Test
	@DisplayName("should return an empty search result when no artifact matches the filter")
	void shouldReturnEmptyResultForNamespaceWithNoArtifacts() {
		final var result = publications.artifacts(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("does-not-match-anything")
				.build());

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("should filter artifact search by groupId criteria")
	void shouldFilterArtifactsByGroupIdCriteria() {
		final var result = publications.artifacts(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.criteria(ArtifactKey.GROUP_ID_CRITERIA, "org.springframework.modulith")
				.build());

		assertThat(result.stream())
				.extracting(ArtifactDefinition::id)
				.containsExactlyInAnyOrder(EntityId.from(9), EntityId.from(10));
	}

	@Test
	@DisplayName("should filter artifact search by artifactId criteria")
	void shouldFilterArtifactsByArtifactIdCriteria() {
		final var result = publications.artifacts(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.criteria(ArtifactKey.ARTIFACT_ID_CRITERIA, "spring-boot-jooq")
				.build());

		assertThat(result.stream())
				.extracting(ArtifactDefinition::id)
				.containsExactly(EntityId.from(11));
	}

	@Test
	@DisplayName("should filter artifact search by term")
	void shouldFilterArtifactsByTerm() {
		final var result = publications.artifacts(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("crypto")
				.build());

		assertThat(result.stream())
				.extracting(ArtifactDefinition::id)
				.containsExactlyInAnyOrder(EntityId.from(2), EntityId.from(3));
	}

	@Test
	@DisplayName("should retrieve an artifact definition owned by the namespace")
	void shouldGetOwnedArtifactDefinition() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-api");

		assertThat(publications.get(Owners.konfigyr(), key))
				.isPresent()
				.get(InstanceOfAssertFactories.type(ArtifactDefinition.class))
				.returns(EntityId.from(5), ArtifactDefinition::id)
				.returns(Owners.konfigyr(), ArtifactDefinition::owner)
				.returns(ArtifactVisibility.PUBLIC, ArtifactDefinition::visibility);
	}

	@Test
	@DisplayName("should retrieve a private artifact definition owned by the namespace")
	void shouldGetOwnedPrivateArtifactDefinition() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-internal-secrets");

		assertThat(publications.get(Owners.konfigyr(), key))
				.isPresent()
				.get(InstanceOfAssertFactories.type(ArtifactDefinition.class))
				.returns(ArtifactVisibility.PRIVATE, ArtifactDefinition::visibility);
	}

	@Test
	@DisplayName("should never retrieve an artifact definition owned by a different namespace, even when public")
	void shouldNotGetArtifactDefinitionOwnedByDifferentNamespace() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-crypto-api");

		assertThat(publications.get(Owners.johnDoe(), key))
				.as("konfigyr-crypto-api is PUBLIC but owned by konfigyr, not john-doe")
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to retrieve an unknown artifact definition")
	void shouldNotGetUnknownArtifactDefinition() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-does-not-exist");

		assertThat(publications.get(Owners.konfigyr(), key)).isEmpty();
	}

	@Test
	@DisplayName("should determine existence of an artifact owned by the namespace")
	void shouldDetermineExistenceOfOwnedArtifact() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-api");

		assertThat(publications.exists(Owners.konfigyr(), key)).isTrue();
	}

	@Test
	@DisplayName("should never determine existence of an artifact owned by a different namespace, even when public")
	void shouldNotDetermineExistenceOfArtifactOwnedByDifferentNamespace() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-crypto-api");

		assertThat(publications.exists(Owners.johnDoe(), key)).isFalse();
	}

	@Test
	@DisplayName("should determine non-existence of an unknown artifact")
	void shouldNotDetermineExistenceOfUnknownArtifact() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-does-not-exist");

		assertThat(publications.exists(Owners.konfigyr(), key)).isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should deregister an owned artifact together with all of its versions")
	void shouldDeregisterOwnedArtifact(AssertablePublishedEvents events) {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-crypto-tink");
		final var coordinates = ArtifactCoordinates.of(key, "1.0.0");

		publications.deregister(Owners.konfigyr(), key);

		assertThat(publications.exists(Owners.konfigyr(), key))
				.as("the artifact definition should be removed")
				.isFalse();

		assertThat(publications.exists(Owners.konfigyr(), coordinates))
				.as("its versions should be cascade removed as well")
				.isFalse();

		events.assertThat()
				.contains(ArtifactoryEvent.Deregistered.class)
				.matching(ArtifactoryEvent.Deregistered::key, key)
				.matching(ArtifactoryEvent.Deregistered::owner, Owners.konfigyr());
	}

	@Test
	@DisplayName("should fail to deregister an unknown artifact")
	void shouldFailToDeregisterUnknownArtifact() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-does-not-exist");

		assertThatExceptionOfType(ArtifactDefinitionNotFoundException.class)
				.isThrownBy(() -> publications.deregister(Owners.konfigyr(), key))
				.returns(key, ArtifactDefinitionNotFoundException::getKey);
	}

	@Test
	@Transactional
	@DisplayName("should fail to deregister an artifact owned by a different namespace")
	void shouldFailToDeregisterArtifactOwnedByDifferentNamespace() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-api");

		assertThatExceptionOfType(ArtifactDefinitionNotFoundException.class)
				.isThrownBy(() -> publications.deregister(Owners.johnDoe(), key))
				.returns(key, ArtifactDefinitionNotFoundException::getKey);

		assertThat(publications.exists(Owners.konfigyr(), key))
				.as("the artifact must remain untouched after a rejected attempt")
				.isTrue();
	}

	@Test
	@DisplayName("should search artifact versions owned by a namespace")
	void shouldSearchVersionsOwnedByNamespace() {
		final var result = publications.versions(Owners.konfigyr(), SearchQuery.of(Pageable.ofSize(20)));

		assertThat(result.stream())
				.extracting(VersionedArtifact::id)
				.containsExactlyInAnyOrder(
						EntityId.from(2), EntityId.from(3), EntityId.from(4), EntityId.from(5),
						EntityId.from(6), EntityId.from(7), EntityId.from(8), EntityId.from(9),
						EntityId.from(10), EntityId.from(11), EntityId.from(12), EntityId.from(13),
						EntityId.from(14), EntityId.from(15), EntityId.from(17), EntityId.from(18)
				);
	}

	@Test
	@DisplayName("should never include another namespace's artifact versions in a search, even when public")
	void shouldNeverIncludeVersionsOwnedByDifferentNamespaceInSearch() {
		final var result = publications.versions(Owners.johnDoe(), SearchQuery.of(Pageable.ofSize(20)));

		assertThat(result.stream())
				.extracting(VersionedArtifact::id)
				.containsExactlyInAnyOrder(EntityId.from(1), EntityId.from(16));
	}

	@Test
	@DisplayName("should filter artifact version search by artifactId criteria")
	void shouldFilterVersionsByArtifactIdCriteria() {
		final var result = publications.versions(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.criteria(ArtifactKey.ARTIFACT_ID_CRITERIA, "spring-boot-jooq")
				.build());

		assertThat(result.stream())
				.extracting(VersionedArtifact::id)
				.containsExactly(EntityId.from(12));
	}

	@Test
	@DisplayName("should filter artifact version search by version criteria")
	void shouldFilterVersionsByVersionCriteria() {
		final var result = publications.versions(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.criteria(ArtifactCoordinates.VERSION_CRITERIA, "1.0.1")
				.build());

		assertThat(result.stream())
				.extracting(VersionedArtifact::id)
				.containsExactly(EntityId.from(3));
	}

	@Test
	@DisplayName("should filter artifact version search by term")
	void shouldFilterVersionsByTerm() {
		final var result = publications.versions(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("modulith")
				.build());

		assertThat(result.stream())
				.extracting(VersionedArtifact::id)
				.containsExactlyInAnyOrder(EntityId.from(10), EntityId.from(11), EntityId.from(14), EntityId.from(15));
	}

	@Test
	@DisplayName("should retrieve an artifact version owned by the namespace")
	void shouldGetOwnedArtifactVersion() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		assertThat(publications.get(Owners.konfigyr(), coordinates))
				.isPresent()
				.get(InstanceOfAssertFactories.type(VersionedArtifact.class))
				.returns(EntityId.from(2), VersionedArtifact::id)
				.returns(Owners.konfigyr(), VersionedArtifact::owner);
	}

	@Test
	@DisplayName("should never retrieve an artifact version owned by a different namespace, even when public")
	void shouldNotGetArtifactVersionOwnedByDifferentNamespace() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		assertThat(publications.get(Owners.johnDoe(), coordinates)).isEmpty();
	}

	@Test
	@DisplayName("should fail to retrieve an unknown artifact version")
	void shouldNotGetUnknownArtifactVersion() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:99.0.0");

		assertThat(publications.get(Owners.konfigyr(), coordinates)).isEmpty();
	}

	@Test
	@DisplayName("should determine existence of an artifact version owned by the namespace")
	void shouldDetermineExistenceOfOwnedArtifactVersion() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		assertThat(publications.exists(Owners.konfigyr(), coordinates)).isTrue();
	}

	@Test
	@DisplayName("should never determine existence of an artifact version owned by a different namespace, even when public")
	void shouldNotDetermineExistenceOfArtifactVersionOwnedByDifferentNamespace() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		assertThat(publications.exists(Owners.johnDoe(), coordinates)).isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should create a new artifact release")
	void shouldPublishArtifact(AssertablePublishedEvents events) {
		final var artifact = TestArtifacts.artifact(builder -> builder.version("3.0.0"));
		final var metadata = TestArtifacts.metadata(artifact);

		assertThat(publications.publish(Owners.konfigyr(), metadata))
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
				.contains(ArtifactoryEvent.PublicationCreated.class)
				.matching(ArtifactoryEvent.PublicationCreated::coordinates, ArtifactCoordinates.of(artifact))
				.matching(ArtifactoryEvent.PublicationCreated::owner, Owners.konfigyr());
	}

	@Test
	@DisplayName("should fail to create a new artifact release when version already exists")
	void shouldFailToPublishExistingVersion(AssertablePublishedEvents events) {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:1.0.0");
		final var metadata = TestArtifacts.metadata(coordinates);

		assertThatExceptionOfType(ArtifactVersionExistsException.class)
				.isThrownBy(() -> publications.publish(Owners.konfigyr(), metadata))
				.returns(coordinates, ArtifactVersionExistsException::getCoordinates);

		assertThat(events.ofType(ArtifactoryEvent.PublicationCreated.class)).isEmpty();
	}

	@Test
	@DisplayName("should fail to release an artifact when the owner has no active claim covering the groupId")
	void shouldFailToPublishForUnverifiedGroupId(AssertablePublishedEvents events) {
		final var coordinates = ArtifactCoordinates.parse("com.unverified:some-artifact:1.0.0");
		final var metadata = TestArtifacts.metadata(coordinates);

		assertThatExceptionOfType(GroupIdNotVerifiedException.class)
				.isThrownBy(() -> publications.publish(Owners.konfigyr(), metadata))
				.returns("com.unverified", GroupIdNotVerifiedException::getGroupId);

		assertThat(events.ofType(ArtifactoryEvent.PublicationCreated.class)).isEmpty();
	}

	@Test
	@DisplayName("should reject publishing a new version when the existing artifact is owned by a different namespace")
	void shouldRejectPublishWhenExistingArtifactOwnedByDifferentNamespace(AssertablePublishedEvents events) {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:reclaimed-artifact:2.0.0");
		final var metadata = TestArtifacts.metadata(coordinates);

		assertThatExceptionOfType(ArtifactOwnershipMismatchException.class)
				.isThrownBy(() -> publications.publish(Owners.konfigyr(), metadata))
				.returns("com.konfigyr", ArtifactOwnershipMismatchException::getGroupId)
				.returns("reclaimed-artifact", ArtifactOwnershipMismatchException::getArtifactId)
				.returns(HttpStatus.CONFLICT, ArtifactOwnershipMismatchException::getStatusCode);

		assertThat(publications.exists(Owners.konfigyr(), coordinates))
				.as("no new version should have been created for the rejected publish attempt")
				.isFalse();

		assertThat(events.ofType(ArtifactoryEvent.PublicationCreated.class)).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should retract a single owned artifact version, leaving the definition and other versions untouched")
	void shouldRetractOwnedVersion(AssertablePublishedEvents events) {
		final var key = ArtifactKey.of("org.springframework.boot", "spring-boot-jooq");
		final var coordinates = ArtifactCoordinates.of(key, "4.0.4");

		publications.retract(Owners.konfigyr(), coordinates);

		assertThat(publications.exists(Owners.konfigyr(), coordinates))
				.as("the retracted version should no longer exist")
				.isFalse();

		assertThat(publications.exists(Owners.konfigyr(), key))
				.as("the artifact definition itself should remain")
				.isTrue();

		events.assertThat()
				.contains(ArtifactoryEvent.PublicationRetracted.class)
				.matching(ArtifactoryEvent.PublicationRetracted::coordinates, coordinates)
				.matching(ArtifactoryEvent.PublicationRetracted::owner, Owners.konfigyr());
	}

	@Test
	@DisplayName("should fail to retract an unknown artifact version")
	void shouldFailToRetractUnknownVersion() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:99.0.0");

		assertThatExceptionOfType(ArtifactVersionNotFoundException.class)
				.isThrownBy(() -> publications.retract(Owners.konfigyr(), coordinates))
				.returns(coordinates, ArtifactVersionNotFoundException::getCoordinates);
	}

	@Test
	@Transactional
	@DisplayName("should fail to retract an artifact version owned by a different namespace")
	void shouldFailToRetractVersionOwnedByDifferentNamespace() {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.0");

		assertThatExceptionOfType(ArtifactVersionNotFoundException.class)
				.isThrownBy(() -> publications.retract(Owners.johnDoe(), coordinates))
				.returns(coordinates, ArtifactVersionNotFoundException::getCoordinates);

		assertThat(publications.exists(Owners.konfigyr(), coordinates))
				.as("the version must remain untouched after a rejected attempt")
				.isTrue();
	}

	@Test
	@Transactional
	@DisplayName("should change visibility of an owned artifact")
	void shouldChangeVisibilityOfOwnedArtifact(AssertablePublishedEvents events) {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-internal-secrets");

		publications.changeVisibility(Owners.konfigyr(), key, ArtifactVisibility.PUBLIC);

		assertThat(publications.get(Owners.konfigyr(), key))
				.isPresent()
				.get(InstanceOfAssertFactories.type(ArtifactDefinition.class))
				.returns(ArtifactVisibility.PUBLIC, ArtifactDefinition::visibility);

		events.assertThat()
				.contains(ArtifactoryEvent.VisibilityChanged.class)
				.matching(ArtifactoryEvent.VisibilityChanged::key, key)
				.matching(ArtifactoryEvent.VisibilityChanged::owner, Owners.konfigyr())
				.matching(ArtifactoryEvent.VisibilityChanged::visibility, ArtifactVisibility.PUBLIC);
	}

	@Test
	@DisplayName("should fail to change visibility of an unknown artifact")
	void shouldFailToChangeVisibilityOfUnknownArtifact() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-does-not-exist");

		assertThatExceptionOfType(ArtifactDefinitionNotFoundException.class)
				.isThrownBy(() -> publications.changeVisibility(Owners.konfigyr(), key, ArtifactVisibility.PUBLIC))
				.returns(key, ArtifactDefinitionNotFoundException::getKey);
	}

	@Test
	@Transactional
	@DisplayName("should fail to change visibility of an artifact owned by a different namespace")
	void shouldFailToChangeVisibilityForDifferentNamespace() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-internal-secrets");

		assertThatExceptionOfType(ArtifactDefinitionNotFoundException.class)
				.isThrownBy(() -> publications.changeVisibility(Owners.johnDoe(), key, ArtifactVisibility.PUBLIC))
				.returns(key, ArtifactDefinitionNotFoundException::getKey);

		assertThat(publications.get(Owners.konfigyr(), key))
				.as("visibility must remain unchanged after a rejected attempt")
				.isPresent()
				.get(InstanceOfAssertFactories.type(ArtifactDefinition.class))
				.returns(ArtifactVisibility.PRIVATE, ArtifactDefinition::visibility);
	}

}
