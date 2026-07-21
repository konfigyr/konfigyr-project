package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ArtifactoryTest extends AbstractIntegrationTest {

	@Autowired
	Artifactory artifactory;

	@Test
	@DisplayName("should retrieve the artifact overview strictly scoped to its owner")
	void shouldGetArtifactDefinitionForOwner() {
		assertThat(artifactory.get(Owners.konfigyr(), ArtifactKey.of("com.konfigyr", "konfigyr-api")))
				.isPresent()
				.get(InstanceOfAssertFactories.type(ArtifactDefinition.class))
				.returns("com.konfigyr", ArtifactDefinition::groupId)
				.returns("konfigyr-api", ArtifactDefinition::artifactId)
				.returns(ArtifactVisibility.PUBLIC, ArtifactDefinition::visibility)
				.returns("Konfigyr API", ArtifactDefinition::name)
				.returns("Private REST API", ArtifactDefinition::description);
	}

	@Test
	@DisplayName("should fail to retrieve the artifact overview for unknown coordinates")
	void shouldFailToGetArtifactDefinitionForUnknownCoordinates() {
		assertThat(artifactory.get(Owners.konfigyr(), ArtifactKey.of("com.konfigyr", "konfigyr-does-not-exist")))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to retrieve the artifact overview when owned by a different namespace")
	void shouldFailToGetArtifactDefinitionOwnedByDifferentNamespace() {
		assertThat(artifactory.get(Owners.konfigyr(), ArtifactKey.of("com.konfigyr", "reclaimed-artifact")))
				.as("the artifact is owned by john-doe, not konfigyr, despite the matching groupId")
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve an artifact definition for a given key regardless of owner")
	void shouldGetArtifactDefinitionForKey() {
		assertThat(artifactory.get(ArtifactKey.of("com.konfigyr", "konfigyr-internal-secrets")))
				.isPresent()
				.get(InstanceOfAssertFactories.type(ArtifactDefinition.class))
				.returns(ArtifactVisibility.PRIVATE, ArtifactDefinition::visibility);
	}

	@Test
	@DisplayName("should fail to retrieve an artifact definition for an unknown key")
	void shouldFailToGetArtifactDefinitionForUnknownKey() {
		assertThat(artifactory.get(ArtifactKey.of("com.konfigyr", "konfigyr-does-not-exist")))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve a public artifact definition for any owner")
	void shouldRetrieveVisiblePublicArtifactDefinition() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-api");

		assertThat(artifactory.get(Owners.johnDoe(), key))
				.isPresent()
				.get(InstanceOfAssertFactories.type(ArtifactDefinition.class))
				.returns(ArtifactVisibility.PUBLIC, ArtifactDefinition::visibility);

		assertThat(artifactory.get(null, key))
				.as("a caller with no namespace context should still see a public artifact definition")
				.isPresent()
				.get(InstanceOfAssertFactories.type(ArtifactDefinition.class))
				.returns(ArtifactVisibility.PUBLIC, ArtifactDefinition::visibility);
	}

	@Test
	@DisplayName("should only retrieve a private artifact definition for its owning namespace")
	void shouldRetrieveVisiblePrivateArtifactDefinitionOnlyForOwner() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-internal-secrets");

		assertThat(artifactory.get(Owners.konfigyr(), key))
				.as("the owning namespace should see its own private artifact definition")
				.isPresent()
				.get(InstanceOfAssertFactories.type(ArtifactDefinition.class))
				.returns(ArtifactVisibility.PRIVATE, ArtifactDefinition::visibility);

		assertThat(artifactory.get(Owners.johnDoe(), key))
				.as("a different namespace should not see a private artifact definition it doesn't own")
				.isEmpty();

		assertThat(artifactory.get(null, key))
				.as("a caller with no namespace context should not see a private artifact definition")
				.isEmpty();
	}

	@Test
	@DisplayName("should determine existence of an artifact for a given key regardless of owner")
	void shouldDetermineExistenceOfArtifactKey() {
		assertThat(artifactory.exists(ArtifactKey.of("com.konfigyr", "konfigyr-internal-secrets"))).isTrue();
	}

	@Test
	@DisplayName("should determine non-existence of an unknown artifact key")
	void shouldDetermineNoExistenceOfUnknownArtifactKey() {
		assertThat(artifactory.exists(ArtifactKey.of("com.konfigyr", "konfigyr-does-not-exist"))).isFalse();
	}

	@Test
	@DisplayName("should determine existence of a public artifact key for any owner")
	void shouldDetermineExistenceOfVisiblePublicArtifactKey() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-api");

		assertThat(artifactory.exists(Owners.johnDoe(), key)).isTrue();

		assertThat(artifactory.exists(null, key))
				.as("a caller with no namespace context should still see a public artifact key exists")
				.isTrue();
	}

	@Test
	@DisplayName("should only determine existence of a private artifact key for its owning namespace")
	void shouldDetermineExistenceOfVisiblePrivateArtifactKeyOnlyForOwner() {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-internal-secrets");

		assertThat(artifactory.exists(Owners.konfigyr(), key))
				.as("the owning namespace should see its own private artifact key exists")
				.isTrue();

		assertThat(artifactory.exists(Owners.johnDoe(), key))
				.as("a different namespace should not see a private artifact key it doesn't own")
				.isFalse();

		assertThat(artifactory.exists(null, key))
				.as("a caller with no namespace context should not see a private artifact key exists")
				.isFalse();
	}

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
	@DisplayName("should search public property definitions for a caller with no namespace context")
	void shouldSearchPublicPropertyDefinitionsForAnyOwner() {
		final var result = artifactory.search(null, SearchQuery.of(Pageable.ofSize(20)));

		assertThat(result.stream())
				.as("only properties owned by PUBLIC artifacts should be visible")
				.extracting(PropertyDefinition::id)
				.containsExactlyInAnyOrder(
						EntityId.from(1), EntityId.from(2), EntityId.from(3), EntityId.from(4),
						EntityId.from(5), EntityId.from(6), EntityId.from(7), EntityId.from(8),
						EntityId.from(9), EntityId.from(10), EntityId.from(11), EntityId.from(12),
						EntityId.from(13), EntityId.from(14), EntityId.from(15), EntityId.from(16)
				);
	}

	@Test
	@DisplayName("should include a namespace's own private property definitions alongside public ones")
	void shouldSearchIncludesOwnPrivatePropertyDefinitions() {
		final var result = artifactory.search(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("internal")
				.build());

		assertThat(result.stream())
				.as("konfigyr should see its own private konfigyr-internal-secrets properties")
				.extracting(PropertyDefinition::id)
				.containsExactlyInAnyOrder(EntityId.from(19), EntityId.from(20), EntityId.from(21));
	}

	@Test
	@DisplayName("should never surface a private property definition owned by a different namespace")
	void shouldSearchNeverIncludesPrivatePropertyDefinitionsOwnedByDifferentNamespace() {
		final var query = SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("internal")
				.build();

		assertThat(artifactory.search(Owners.johnDoe(), query))
				.as("john-doe must not see konfigyr's private properties, even by term match")
				.isEmpty();

		assertThat(artifactory.search(null, query))
				.as("a caller with no namespace context must not see private properties")
				.isEmpty();
	}

	@Test
	@DisplayName("should only surface a private property definition for its owning namespace")
	void shouldSearchPrivatePropertyDefinitionOnlyForOwner() {
		final var query = SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("notes")
				.build();

		assertThat(artifactory.search(Owners.johnDoe(), query).stream())
				.as("john-doe should see its own private-notes properties")
				.extracting(PropertyDefinition::id)
				.containsExactlyInAnyOrder(EntityId.from(17), EntityId.from(18));

		assertThat(artifactory.search(Owners.konfigyr(), query))
				.as("konfigyr must not see john-doe's private properties")
				.isEmpty();

		assertThat(artifactory.search(null, query))
				.as("a caller with no namespace context must not see john-doe's private properties")
				.isEmpty();
	}

	@Test
	@DisplayName("should match a property definition by its description alone, without a name match")
	void shouldSearchMatchPropertyByDescriptionOnly() {
		final var result = artifactory.search(null, SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("logging")
				.build());

		assertThat(result.stream())
				.as("'logging' only appears in spring.application.name's description, never in its name")
				.extracting(PropertyDefinition::id)
				.containsExactly(EntityId.from(1));
	}

	@Test
	@DisplayName("should rank a property definition matching by name above one matching only by description")
	void shouldSearchRankNameMatchAboveDescriptionOnlyMatch() {
		final var result = artifactory.search(null, SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("application")
				.build());

		assertThat(result.stream())
				.as("id=1 and id=11 match 'application' by name, id=12 only matches through its description")
				.extracting(PropertyDefinition::id)
				.containsExactly(EntityId.from(1), EntityId.from(11), EntityId.from(12));
	}

	@Test
	@DisplayName("should match a property definition by a partial, autocomplete-style term")
	void shouldSearchMatchByPartialTerm() {
		final var result = artifactory.search(null, SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("spring.appl")
				.build());

		assertThat(result.stream())
				.as("'spring.appl' should prefix-match 'spring' and 'application' as separate words")
				.extracting(PropertyDefinition::id)
				.containsExactly(EntityId.from(1), EntityId.from(12));
	}

	@Test
	@DisplayName("should filter property definition search by groupId criteria")
	void shouldSearchFilterByGroupIdCriteria() {
		final var result = artifactory.search(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.criteria(ArtifactKey.GROUP_ID_CRITERIA, "org.springframework.modulith")
				.build());

		assertThat(result.stream())
				.extracting(PropertyDefinition::id)
				.containsExactlyInAnyOrder(
						EntityId.from(12), EntityId.from(13), EntityId.from(14),
						EntityId.from(15), EntityId.from(16)
				);
	}

	@Test
	@DisplayName("should filter property definition search by artifactId criteria")
	void shouldSearchFilterByArtifactIdCriteria() {
		final var result = artifactory.search(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.criteria(ArtifactKey.ARTIFACT_ID_CRITERIA, "spring-boot-actuator")
				.build());

		assertThat(result.stream())
				.extracting(PropertyDefinition::id)
				.containsExactlyInAnyOrder(EntityId.from(8), EntityId.from(9), EntityId.from(10), EntityId.from(11));
	}

	@Test
	@DisplayName("should filter property definition search by version criteria")
	void shouldSearchFilterByVersionCriteria() {
		final var matchingBothVersions = artifactory.search(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("internal")
				.criteria(ArtifactCoordinates.VERSION_CRITERIA, "1.0.0")
				.build());

		assertThat(matchingBothVersions.stream())
				.as("id=21 was only ever declared on version 1.1.0, not 1.0.0")
				.extracting(PropertyDefinition::id)
				.containsExactlyInAnyOrder(EntityId.from(19), EntityId.from(20));

		final var matchingLatestVersion = artifactory.search(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("internal")
				.criteria(ArtifactCoordinates.VERSION_CRITERIA, "1.1.0")
				.build());

		assertThat(matchingLatestVersion.stream())
				.extracting(PropertyDefinition::id)
				.containsExactlyInAnyOrder(EntityId.from(19), EntityId.from(20), EntityId.from(21));
	}

	@Test
	@DisplayName("should return an empty search result when no property definition matches the term")
	void shouldSearchReturnEmptyResultForUnmatchedTerm() {
		final var result = artifactory.search(Owners.konfigyr(), SearchQuery.builder()
				.pageable(Pageable.ofSize(20))
				.term("does-not-match-anything")
				.build());

		assertThat(result).isEmpty();
	}

}
