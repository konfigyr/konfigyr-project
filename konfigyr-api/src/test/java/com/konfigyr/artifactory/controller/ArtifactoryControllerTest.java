package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.*;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class ArtifactoryControllerTest extends AbstractControllerTest {

	@Autowired
	MetadataStore store;

	@Test
	@DisplayName("should retrieve details about the specific artifact release version")
	void retrieveReleasedArtifact() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-api", "1.0.0");

		mvc.get().uri(uriForArtifact(coordinates).toUri())
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(VersionedArtifact.class)
				.returns(EntityId.from(6), VersionedArtifact::id)
				.returns(EntityId.from(5), VersionedArtifact::artifact)
				.returns(coordinates.groupId(), Release::groupId)
				.returns(coordinates.artifactId(), Release::artifactId)
				.returns(coordinates.version().get(), Release::version)
				.returns("Konfigyr API", Release::name)
				.returns("Private REST API", Release::description)
				.returns(URI.create("konfigyr.api"), Release::website)
				.returns(URI.create("https://github.com/konfigyr/konfigyr-project"), Release::repository)
				.returns(List.of(), Release::errors)
				.returns("7FTrQ6LxfT_s9QYsmHx5TqAl2iWN4LbqZINULveeP4o=", Release::checksum)
				.satisfies(it -> assertThat(it.releasedAt())
						.isCloseTo(Instant.now().minus(1, ChronoUnit.HOURS), within(15, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should fail to retrieve details about an unknown specific artifact release version")
	void retrieveUnknownReleasedArtifact() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "unknown", "1.0.0");

		mvc.get().uri(uriForArtifact(coordinates).toUri())
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(hasFailedWithException(ArtifactVersionNotFoundException.class, ex -> ex
						.returns(coordinates, ArtifactVersionNotFoundException::getCoordinates)
						.hasNoCause()
				))
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitleContaining("Artifact not found")
						.hasDetailContaining("Could not find an artifact with the following coordinates: '%s'", coordinates.format())
						.hasProperty("coordinates", coordinates.format())
				));
	}

	@Test
	@DisplayName("should perform an artifact version check on an existing release")
	void shouldCheckExistingArtifactVersion() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-api", "1.0.0");

		mvc.head().uri(uriForArtifact(coordinates).toUri())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.body()
				.isEmpty();
	}

	@Test
	@DisplayName("should perform an artifact version check on an unknown release")
	void shouldCheckUnknownArtifactVersion() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "unknown", "1.0.0");

		mvc.head().uri(uriForArtifact(coordinates).toUri())
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.body()
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should upload new artifact and create a release")
	void shouldUploadNewArtifact() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-api", "3.0.0");
		final var artifact = TestArtifacts.artifact(coordinates, builder -> builder
				.name("Konfigyr API")
				.description("Konfigyr REST API artifact")
				.website("https://api.konfigyr.com")
				.repository("https://github.com/konfigyr/konfigyr-project")
		);
		final var metadata = TestArtifacts.metadata(artifact);

		mvc.post().uri(uriForArtifact(coordinates).toUri())
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(TestArtifacts.metadata(artifact)))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(DefaultRelease.class)
				.returns(coordinates.groupId(), Release::groupId)
				.returns(coordinates.artifactId(), Release::artifactId)
				.returns(coordinates.version().get(), Release::version)
				.returns(artifact.name(), Release::name)
				.returns(artifact.description(), Release::description)
				.returns(artifact.website(), Release::website)
				.returns(artifact.repository(), Release::repository)
				.returns(ReleaseState.PENDING, Release::state)
				.returns(List.of(), Release::errors)
				.returns("jZ1Tz9XSf-v4K68PjYAVRTWMHPIePVTPnC5cW6F1S5g=", Release::checksum)
				.satisfies(it -> assertThat(it.releasedAt())
						.isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS))
				);

		final var resource = store.get(coordinates);
		assertThat(resource)
				.as("Should store property descriptor in the metadata store")
				.isPresent()
				.get()
				.satisfies(it -> {
					final List<PropertyDescriptor> properties = jsonMapper.readValue(
							it.getContentAsByteArray(),
							jsonMapper.getTypeFactory().constructCollectionLikeType(List.class, PropertyDescriptor.class)
					);

					assertThat(properties)
							.as("The stored property descriptors must match the ones in the request")
							.containsExactlyElementsOf(metadata.properties());
				});
	}

	@Test
	@DisplayName("should fail to upload existing artifact")
	void uploadExistingArtifact() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-api", "1.0.0");
		final var metadata = jsonMapper.<ObjectNode>valueToTree(Artifact.builder()
				.artifactId(coordinates.artifactId())
				.groupId(coordinates.groupId())
				.version(coordinates.version().get())
				.build()
		);

		// register an empty object in the properties array to verify that validation is skipped
		// for property descriptors within the payload. The validation of property descriptors
		// should be performed by the artifact metadata release batch job
		metadata.set("properties", jsonMapper.createArrayNode().add(
				jsonMapper.createArrayNode().objectNode()
		).add(
				jsonMapper.createArrayNode().objectNode()
						.put("name", "property.name")
		));

		mvc.post().uri(uriForArtifact(coordinates).toUri())
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(metadata.toPrettyString())
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Artifact already exists")
						.hasDetailContaining("You attempted to create a new artifact with coordinates '%s' that already exists", coordinates.format())
				))
				.satisfies(hasFailedWithException(ArtifactVersionExistsException.class, ex -> ex
						.returns(HttpStatus.BAD_REQUEST, ArtifactVersionExistsException::getStatusCode)
						.returns(coordinates, ArtifactVersionExistsException::getCoordinates)
				));
	}

	@Test
	@DisplayName("should fail to upload invalid artifact metadata")
	void uploadInvalidArtifact() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-api", "3.0.0");

		mvc.post().uri(uriForArtifact(coordinates).toUri())
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(hasFailedWithException(BindException.class))
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid")
						.hasDetailContaining("invalid request data")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("properties", "groupId", "artifactId", "version")
						)
				));
	}

	@Test
	@DisplayName("should fail to upload artifact metadata with non-matching coordinates")
	void uploadArtifactWithMismatchingCoordinates() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-api", "3.0.0");

		mvc.post().uri(uriForArtifact(coordinates).toUri())
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"groupId\": \"org.konfigyr\", \"artifactId\": \"konfigyr\", \"version\": \"1.0.0\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(hasFailedWithException(BindException.class))
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid")
						.hasDetailContaining("invalid request data")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("properties", "groupId", "artifactId", "version")
						)
				));
	}

	@Test
	@DisplayName("should retrieve property descriptors for a specific artifact version")
	void retrieveArtifactProperties() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-crypto-api", "1.0.1");

		mvc.get().uri(uriForArtifact(coordinates, "properties").toUri())
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(collectionModel(PropertyDefinition.class))
				.satisfies(it -> assertThat(it.getContent())
								.hasSize(1)
								.first()
								.returns(EntityId.from(1), PropertyDefinition::id)
								.returns(EntityId.from(2), PropertyDefinition::artifact)
								.returns("spring.application.name", PropertyDefinition::name)
								.returns("java.lang.String", PropertyDefinition::typeName)
								.returns(StringSchema.instance(), PropertyDefinition::schema)
								.returns(ByteArray.fromBase64String("cRJ8jlPpTPmTJEoZEZNDSjvdqafG05QkzNJplXyu9J0="), PropertyDefinition::checksum)
								.returns("Application name. Typically used with logging to help identify the application.", PropertyDefinition::description)
								.returns(null, PropertyDefinition::defaultValue)
								.returns(null, PropertyDefinition::deprecation)
								.returns(1, PropertyDefinition::occurrences)
								.returns(coordinates.version(), PropertyDefinition::firstSeen)
								.returns(coordinates.version(), PropertyDefinition::lastSeen)
				);
	}

	@Test
	@DisplayName("should retrieve an empty property descriptors response when artifact version has no matching properties")
	void retrieveEmptyArtifactProperties() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-crypto-api", "1.0.0");

		mvc.get().uri(uriForArtifact(coordinates, "properties").toUri())
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(collectionModel(PropertyDefinition.class))
				.satisfies(it -> assertThat(it.getContent().isEmpty()))
				.satisfies(it -> assertThat(it.getLinks().isEmpty()));
	}

	@Test
	@DisplayName("should fail to retrieve property descriptors for an unknown artifact")
	void retrievePropertiesForUnknownArtifact() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "unknown", "1.0.0");

		mvc.get().uri(uriForArtifact(coordinates, "properties").toUri())
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(hasFailedWithException(ArtifactVersionNotFoundException.class, ex -> ex
						.returns(coordinates, ArtifactVersionNotFoundException::getCoordinates)
						.hasNoCause()
				))
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitleContaining("Artifact not found")
						.hasDetailContaining("Could not find an artifact with the following coordinates: '%s'", coordinates.format())
						.hasProperty("coordinates", coordinates.format())
				));
	}

	static UriComponents uriForArtifact(ArtifactCoordinates coordinates, String... path) {
		return UriComponentsBuilder.fromPath("/artifacts/{groupId}/{artifactId}/{version}")
				.pathSegment(path)
				.buildAndExpand(coordinates.groupId(), coordinates.artifactId(), coordinates.version().get());
	}

}
