package com.konfigyr.namespace.controller;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectAssert;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceConfigurationCatalog.SERVICE_CONFIGURATION_CATALOG;
import static com.konfigyr.data.tables.ServiceReleases.SERVICE_RELEASES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class ServiceManifestControllerTest extends AbstractControllerTest {

	// matches the checksum seeded for artifact_versions rows referenced by konfigyr-id's manifest, see artifactory.sql
	private static final String EXISTING_ARTIFACT_VERSION_CHECKSUM = "ec54eb43a2f17d3fecf5062c987c794ea025da258de0b6ea6483542ef79e3f8a";

	static Artifact konfigyrArtifactoryArtifact = Artifact.of("com.konfigyr", "konfigyr-artifactory", "1.2.0");
	static Artifact konfigyrCryptoApiArtifact = Artifact.of("com.konfigyr", "konfigyr-crypto-api", "1.1.0");

	@Autowired
	DSLContext context;

	@Test
	@DisplayName("should retrieve the latest namespace service manifest")
	void retrieveServiceManifest() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(Manifest.class)
				.returns(EntityId.from(1).serialize(), Manifest::id)
				.returns("Konfigyr ID", Manifest::name)
				.satisfies(it -> assertThat(it.artifacts())
						.hasSize(8)
						.usingRecursiveFieldByFieldElementComparatorIgnoringFields("resolvedAt")
						.containsExactly(
								ManifestEntry.builder()
										.groupId("com.acme")
										.artifactId("spring-boot-service")
										.version("1.0.0")
										.name("Acme Spring Boot service")
										.description("Spring Boot service")
										.website("https://acme.com/service")
										.repository("https://github.com/acme/service")
										.checksum("6QRgbo04ZnpKhc3o5yZckptP+61bzEBhwNibipufooU=")
										.source(ArtifactSource.LOCAL)
										.resolvedAt(Instant.EPOCH)
										.build(),
								ManifestEntry.builder()
										.groupId("org.springframework.boot")
										.artifactId("spring-boot")
										.version("4.0.4")
										.name("Spring Boot")
										.description("Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications")
										.website("https://spring.io/projects/spring-boot")
										.repository("https://github.com/spring-projects/spring-boot")
										.checksum(EXISTING_ARTIFACT_VERSION_CHECKSUM)
										.source(ArtifactSource.ARTIFACTORY)
										.resolvedAt(Instant.EPOCH)
										.build(),
								ManifestEntry.builder()
										.groupId("org.springframework.boot")
										.artifactId("spring-boot-actuator")
										.version("4.0.4")
										.name("Spring Boot Actuator")
										.description("Spring Boot Actuator")
										.website("https://spring.io/projects/spring-boot")
										.repository("https://github.com/spring-projects/spring-boot")
										.checksum(EXISTING_ARTIFACT_VERSION_CHECKSUM)
										.source(ArtifactSource.ARTIFACTORY)
										.resolvedAt(Instant.EPOCH)
										.build(),
								ManifestEntry.builder()
										.groupId("org.springframework.boot")
										.artifactId("spring-boot-autoconfigure")
										.version("4.0.4")
										.name("Spring Boot AutoConfigure")
										.description("Spring Boot auto-configuration attempts to automatically configure your Spring applications")
										.website("https://spring.io/projects/spring-boot")
										.repository("https://github.com/spring-projects/spring-boot")
										.checksum(EXISTING_ARTIFACT_VERSION_CHECKSUM)
										.source(ArtifactSource.ARTIFACTORY)
										.resolvedAt(Instant.EPOCH)
										.build(),
								ManifestEntry.builder()
										.groupId("org.springframework.boot")
										.artifactId("spring-boot-jooq")
										.version("4.0.4")
										.name("Spring Boot jOOQ")
										.description("Spring Boot jOOQ support")
										.website("https://spring.io/projects/spring-boot")
										.repository("https://github.com/spring-projects/spring-boot")
										.checksum(EXISTING_ARTIFACT_VERSION_CHECKSUM)
										.source(ArtifactSource.ARTIFACTORY)
										.resolvedAt(Instant.EPOCH)
										.build(),
								ManifestEntry.builder()
										.groupId("org.springframework.boot")
										.artifactId("spring-boot-liquibase")
										.version("4.0.4")
										.name("Spring Boot Liquibase")
										.description("Spring Boot Liquibase support")
										.website("https://spring.io/projects/spring-boot")
										.repository("https://github.com/spring-projects/spring-boot")
										.checksum(EXISTING_ARTIFACT_VERSION_CHECKSUM)
										.source(ArtifactSource.ARTIFACTORY)
										.resolvedAt(Instant.EPOCH)
										.build(),
								ManifestEntry.builder()
										.groupId("org.springframework.modulith")
										.artifactId("spring-modulith-core")
										.version("2.0.3")
										.name("Spring Modulith Core")
										.description("Modular monoliths with Spring Boot")
										.website("https://spring.io/projects/spring-modulith/spring-modulith-core")
										.repository("https://github.com/spring-projects-experimental/spring-modulith")
										.checksum(EXISTING_ARTIFACT_VERSION_CHECKSUM)
										.source(ArtifactSource.ARTIFACTORY)
										.resolvedAt(Instant.EPOCH)
										.build(),
								ManifestEntry.builder()
										.groupId("org.springframework.modulith")
										.artifactId("spring-modulith-moments")
										.version("2.0.3")
										.name("Spring Modulith Moments")
										.description("Modular monoliths with Spring Boot")
										.website("https://spring.io/projects/spring-modulith/spring-modulith-moments")
										.repository("https://github.com/spring-projects-experimental/spring-modulith")
										.checksum(EXISTING_ARTIFACT_VERSION_CHECKSUM)
										.source(ArtifactSource.ARTIFACTORY)
										.resolvedAt(Instant.EPOCH)
										.build()
						)
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(Instant.now().minus(3, ChronoUnit.DAYS), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should retrieve an empty manifest for service that was not yet released")
	void retrieveManifestForUnreleasedService() {
		mvc.get().uri("/namespaces/john-doe/services/john-doe-blog/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(Manifest.class)
				.returns(EntityId.from(1).serialize(), Manifest::id)
				.returns("John Doe Blog", Manifest::name)
				.returns(List.of(), Manifest::artifacts)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(Instant.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should fail to retrieve manifest for unknown service")
	void retrieveManifestForUnknownService() {
		mvc.get().uri("/namespaces/konfigyr/services/unknown-service/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not retrieve a service manifest for an unknown namespace")
	void retrieveManifestForUnknownNamespace() {
		mvc.get().uri("/namespaces/unknown-namespace/services/unknown-service/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should not retrieve service manifest when namespaces:read scope is not present")
	void retrieveManifestWithoutScope() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id/manifest")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should not retrieve service manifest when user is not a member of a namespace")
	void retrieveManifestWithoutMembership() {
		mvc.get().uri("/namespaces/john-doe/services/john-doe-blog/manifest")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should open a new release when the service has none")
	void shouldOpenNewRelease() {
		final var candidates = List.of(
				ServiceReleaseCandidate.of(konfigyrArtifactoryArtifact, "checksum-1"),
				ServiceReleaseCandidate.of(konfigyrCryptoApiArtifact, "checksum-2")
		);

		assertThatRelease(candidates)
				.returns(com.konfigyr.artifactory.ReleaseState.PENDING, ServiceRelease::state)
				.satisfies(release -> assertThat(release.artifacts())
						.hasSize(2)
						.satisfiesExactlyInAnyOrder(
								entry -> assertThat(entry)
										.returns(konfigyrArtifactoryArtifact.groupId(), Artifact::groupId)
										.returns(konfigyrArtifactoryArtifact.artifactId(), Artifact::artifactId)
										.returns(konfigyrArtifactoryArtifact.version(), Artifact::version)
										.returns(ArtifactUploadStatus.UPLOAD_REQUIRED, ServiceReleaseEntry::status),
								entry -> assertThat(entry)
										.returns(konfigyrCryptoApiArtifact.groupId(), Artifact::groupId)
										.returns(konfigyrCryptoApiArtifact.artifactId(), Artifact::artifactId)
										.returns(konfigyrCryptoApiArtifact.version(), Artifact::version)
										.returns(ArtifactUploadStatus.UPLOAD_REQUIRED, ServiceReleaseEntry::status)
						)
				);
	}

	@Test
	@Transactional
	@DisplayName("should skip an artifact that is already indexed in the Artifactory")
	void shouldSkipIndexedArtifact() {
		assertThatRelease(ServiceReleaseCandidate.of("com.konfigyr", "konfigyr-crypto-api", "1.0.0", "checksum"))
				.satisfies(release -> assertThat(release.artifacts())
						.hasSize(1)
						.first()
						.returns("com.konfigyr", Artifact::groupId)
						.returns("konfigyr-crypto-api", Artifact::artifactId)
						.returns("1.0.0", Artifact::version)
						.returns(ArtifactUploadStatus.SKIP, ServiceReleaseEntry::status)
				);

		// assert that artifact is added to the release artifacts with the matching source
		final var record = context.select(SERVICE_ARTIFACTS.SOURCE, SERVICE_ARTIFACTS.CHECKSUM)
				.from(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.GROUP_ID.eq("com.konfigyr"))
				.and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq("konfigyr-crypto-api"))
				.and(SERVICE_ARTIFACTS.VERSION.eq("1.0.0"))
				.fetchOneMap();

		assertThat(record)
				.containsEntry(SERVICE_ARTIFACTS.SOURCE.getName(), ArtifactSource.ARTIFACTORY.name())
				.containsEntry(SERVICE_ARTIFACTS.CHECKSUM.getName(), null);
	}

	@Test
	@Transactional
	@DisplayName("should take over an existing release and skip artifacts with a matching checksum")
	void shouldTakeOverExistingRelease() {
		final var metadata = metadata(konfigyrArtifactoryArtifact, "checksum-1");

		final ServiceRelease first = assertThatRelease(ServiceReleaseCandidate.of(metadata))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		// checksum-1 is what the plugin already declared for this coordinate above, so uploading
		// metadata carrying that same checksum is what settles it as "already uploaded"
		assertThatUpload(first.id(), metadata)
				.hasStatus(HttpStatus.NO_CONTENT);

		assertThatRelease(ServiceReleaseCandidate.of(metadata))
				.returns(first.id(), ServiceRelease::id)
				.extracting(ServiceRelease::artifacts, InstanceOfAssertFactories.iterable(ServiceReleaseEntry.class))
				.hasSize(1)
				.first()
				.returns(ArtifactUploadStatus.SKIP, ServiceReleaseEntry::status);
	}

	@Test
	@Transactional
	@DisplayName("should mark an artifact upload required when its checksum changed")
	void shouldRequireUploadWhenChecksumChanged() {
		final var metadata = metadata(konfigyrArtifactoryArtifact, "initial-checksum");

		final var release = assertThatRelease(ServiceReleaseCandidate.of(metadata))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		// `initial-checksum` is what the plugin already declared for this coordinate above, so uploading
		// metadata carrying that same checksum is what settles it as "already uploaded"
		assertThatUpload(release.id(), metadata)
				.hasStatus(HttpStatus.NO_CONTENT);

		assertThatRelease(ServiceReleaseCandidate.of(konfigyrArtifactoryArtifact, "different-checksum"))
				.returns(release.id(), ServiceRelease::id)
				.extracting(ServiceRelease::artifacts, InstanceOfAssertFactories.iterable(ServiceReleaseEntry.class))
				.hasSize(1)
				.first()
				.returns(ArtifactUploadStatus.UPLOAD_REQUIRED, ServiceReleaseEntry::status);
	}

	@Test
	@Transactional
	@DisplayName("should prune artifacts that are no longer declared")
	void shouldPruneStaleArtifacts() {
		assertThatRelease(List.of(
				ServiceReleaseCandidate.of(konfigyrArtifactoryArtifact, "checksum-1"),
				ServiceReleaseCandidate.of(konfigyrCryptoApiArtifact, "checksum-2")
		)).satisfies(release -> assertThat(release.artifacts()).hasSize(2));

		assertThatRelease(ServiceReleaseCandidate.of(konfigyrArtifactoryArtifact, "checksum-1"))
				.satisfies(release -> assertThat(release.artifacts()).hasSize(1));

		assertThat(context.fetchExists(SERVICE_ARTIFACTS, DSL.and(
				SERVICE_ARTIFACTS.GROUP_ID.eq(konfigyrCryptoApiArtifact.groupId()),
				SERVICE_ARTIFACTS.ARTIFACT_ID.eq(konfigyrCryptoApiArtifact.artifactId()),
				SERVICE_ARTIFACTS.VERSION.eq(konfigyrCryptoApiArtifact.version())
		))).isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should upload artifact metadata for a declared coordinate")
	void shouldUploadArtifactMetadata() {
		final var metadata = konfigyrArtifactoryArtifact.toMetadata(List.of(
				PropertyDescriptor.builder()
						.name("com.konfigyr.konfigyr-artifactory.enabled")
						.typeName("java.lang.Boolean")
						.schema(BooleanSchema.instance())
						.description("Enables the artifact metadata upload")
						.defaultValue("true")
						.build()
		));

		final var release = assertThatRelease(ServiceReleaseCandidate.of(metadata))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		assertThatUpload(release.id(), metadata)
				.hasStatus(HttpStatus.NO_CONTENT);

		final var record = context.select(SERVICE_ARTIFACTS.SOURCE, SERVICE_ARTIFACTS.CHECKSUM)
				.from(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.GROUP_ID.eq(konfigyrArtifactoryArtifact.groupId()))
				.and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq(konfigyrArtifactoryArtifact.artifactId()))
				.and(SERVICE_ARTIFACTS.VERSION.eq(konfigyrArtifactoryArtifact.version()))
				.fetchOneMap();

		assertThat(record)
				.containsEntry(SERVICE_ARTIFACTS.SOURCE.getName(), ArtifactSource.LOCAL.name())
				.containsEntry(SERVICE_ARTIFACTS.CHECKSUM.getName(), metadata.checksum());

		final var properties = context.select(SERVICE_CONFIGURATION_CATALOG.fields())
				.from(SERVICE_CONFIGURATION_CATALOG)
				.where(SERVICE_CONFIGURATION_CATALOG.GROUP_ID.eq(metadata.groupId()))
				.and(SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID.eq(metadata.artifactId()))
				.and(SERVICE_CONFIGURATION_CATALOG.VERSION.eq(metadata.version()))
				.fetch(Record::intoMap);

		assertThat(properties)
				.hasSize(1)
				.first(InstanceOfAssertFactories.map(String.class, Object.class))
				.containsEntry(SERVICE_CONFIGURATION_CATALOG.NAME.getName(), "com.konfigyr.konfigyr-artifactory.enabled")
				.containsEntry(SERVICE_CONFIGURATION_CATALOG.TYPE_NAME.getName(), "java.lang.Boolean")
				.containsEntry(SERVICE_CONFIGURATION_CATALOG.DEFAULT_VALUE.getName(), "true")
				.containsEntry(SERVICE_CONFIGURATION_CATALOG.DESCRIPTION.getName(), "Enables the artifact metadata upload")
				.containsEntry(SERVICE_CONFIGURATION_CATALOG.DEPRECATION.getName(), null)
				.hasEntrySatisfying(SERVICE_CONFIGURATION_CATALOG.SCHEMA.getName(), it -> assertThat(it)
						.isNotNull()
						.isInstanceOf(ByteArray.class)
						.asInstanceOf(InstanceOfAssertFactories.type(ByteArray.class))
						.returns("{\"type\":\"boolean\"}", bytes -> bytes.toString(StandardCharsets.UTF_8))
				)
				.hasEntrySatisfying(SERVICE_CONFIGURATION_CATALOG.SEARCH_VECTOR.getName(), it -> assertThat(it)
						.isNotNull()
				);
	}

	@Test
	@Transactional
	@DisplayName("should upload artifact metadata containing a large number of properties")
	void shouldUploadLargeArtifactMetadata() throws IOException {
		final ArtifactMetadata metadata = jsonMapper.readValue(
				new ClassPathResource("fixtures/org.springframework.boot-spring-boot-autoconfigure-3.5.7.json").getInputStream(),
				ArtifactMetadata.class
		);

		final var release = assertThatRelease(ServiceReleaseCandidate.of(metadata))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		final Instant start = Instant.now();

		assertThatUpload(release.id(), metadata)
				.hasStatus(HttpStatus.NO_CONTENT);

		// loose sanity bound, not a precise perf assertion: catches a genuine regression
		// (e.g. row-by-row inserts instead of a bulk insert) without flaking on CI noise
		assertThat(Duration.between(start, Instant.now()))
				.as("Uploading a large artifact metadata payload should complete quickly")
				.isLessThan(Duration.ofSeconds(5));

		assertThat(context.fetchCount(SERVICE_CONFIGURATION_CATALOG,
				SERVICE_CONFIGURATION_CATALOG.GROUP_ID.eq(metadata.groupId())
						.and(SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID.eq(metadata.artifactId()))
						.and(SERVICE_CONFIGURATION_CATALOG.VERSION.eq(metadata.version()))
		)).isEqualTo(metadata.properties().size());
	}

	@Test
	@Transactional
	@DisplayName("should replace catalog rows and update the checksum when the same coordinate is uploaded again")
	void shouldReplaceCatalogRowsOnReupload() {
		final var metadata = konfigyrArtifactoryArtifact.toMetadata(List.of(
				PropertyDescriptor.builder()
						.name("com.konfigyr.konfigyr-artifactory.enabled")
						.typeName("java.lang.Boolean")
						.schema(StringSchema.instance())
						.build()
		));

		final var release = assertThatRelease(ServiceReleaseCandidate.of(metadata))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		assertThatUpload(release.id(), metadata)
				.hasStatus(HttpStatus.NO_CONTENT);

		assertThat(context.select(SERVICE_CONFIGURATION_CATALOG.NAME)
				.from(SERVICE_CONFIGURATION_CATALOG)
				.where(SERVICE_CONFIGURATION_CATALOG.GROUP_ID.eq(metadata.groupId()))
				.and(SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID.eq(metadata.artifactId()))
				.and(SERVICE_CONFIGURATION_CATALOG.VERSION.eq(metadata.version()))
				.fetch(SERVICE_CONFIGURATION_CATALOG.NAME))
				.containsExactly("com.konfigyr.konfigyr-artifactory.enabled");

		assertThat(context.select(SERVICE_ARTIFACTS.CHECKSUM)
				.from(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.GROUP_ID.eq(metadata.groupId()))
				.and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq(metadata.artifactId()))
				.and(SERVICE_ARTIFACTS.VERSION.eq(metadata.version()))
				.fetchOne(SERVICE_ARTIFACTS.CHECKSUM))
				.isEqualTo(metadata.checksum());
	}

	@Test
	@Transactional
	@DisplayName("should reject an artifact upload when the release is not pending")
	void shouldRejectUploadForNotPendingRelease() {
		final var release = assertThatRelease(ServiceReleaseCandidate.of(konfigyrArtifactoryArtifact, "checksum"))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		final int updated = context.update(SERVICE_RELEASES)
				.set(SERVICE_RELEASES.STATE, ReleaseState.RELEASED.name())
				.where(SERVICE_RELEASES.ID.eq(EntityId.from(release.id()).get()))
				.execute();

		assertThat(updated)
				.as("Expected an existing service_releases row to update")
				.isOne();

		assertThatUpload(release.id(), metadata(konfigyrArtifactoryArtifact, "checksum"))
				.satisfies(problemDetailFor(HttpStatus.CONFLICT, problem -> problem
						.hasTitle("Release is not pending")
						.hasDetailContaining("Release is no longer pending")
				));
	}

	@Test
	@Transactional
	@DisplayName("should reject an artifact upload for a coordinate that was not declared for the release")
	void shouldRejectUploadForUndeclaredCoordinate() {
		final var release = assertThatRelease(ServiceReleaseCandidate.of(konfigyrArtifactoryArtifact, "checksum"))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		assertThatUpload(release.id(), metadata(konfigyrCryptoApiArtifact, "crypto-checksum"))
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle("Artifact not declared for this release")
						.hasDetailContaining("The artifact with coordinates '%s' was not declared for this release",
								ArtifactCoordinates.of(konfigyrCryptoApiArtifact).format())
				));
	}

	@Test
	@Transactional
	@DisplayName("should reject a malformed artifact metadata payload")
	void shouldRejectMalformedUploadPayload() {
		final var release = assertThatRelease(ServiceReleaseCandidate.of(konfigyrArtifactoryArtifact, "checksum"))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases/{id}/artifacts", release.id())
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not-valid-json")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("should fail to upload an artifact for an unknown service")
	void shouldRejectUnknownServiceForArtifactUpload() {
		final var metadata = metadata(konfigyrCryptoApiArtifact, "crypto-checksum");

		mvc.post().uri("/namespaces/konfigyr/services/unknown-service/releases/{id}/artifacts", EntityId.from(999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(metadata))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@Transactional
	@DisplayName("should complete a release once every declared artifact has been uploaded")
	void shouldCompleteRelease() {
		final var metadata = metadata(konfigyrArtifactoryArtifact, "checksum");
		final var release = assertThatRelease(ServiceReleaseCandidate.of(metadata))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		assertThatUpload(release.id(), metadata)
				.hasStatus(HttpStatus.NO_CONTENT);

		assertThatComplete(release.id())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.returns(release.id(), ServiceRelease::id)
				.returns(ReleaseState.RELEASED, ServiceRelease::state)
				.returns(List.of(), ServiceRelease::errors)
				.satisfies(completed -> assertThat(completed.publishedAt()).isNotNull());

		assertThat(context.select(SERVICE_RELEASES.STATE, SERVICE_RELEASES.PUBLISHED_AT)
				.from(SERVICE_RELEASES)
				.where(SERVICE_RELEASES.ID.eq(EntityId.from(release.id()).get()))
				.fetchOneMap())
				.containsEntry(SERVICE_RELEASES.STATE.getName(), ReleaseState.RELEASED.name())
				.hasEntrySatisfying(SERVICE_RELEASES.PUBLISHED_AT.getName(), it -> assertThat(it).isNotNull());
	}

	@Test
	@Transactional
	@DisplayName("should fail to complete a release when a declared artifact was never uploaded")
	void shouldFailToCompleteReleaseWithMissingUpload() {
		final var release = assertThatRelease(ServiceReleaseCandidate.of(konfigyrArtifactoryArtifact, "checksum"))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		assertThatComplete(release.id())
				.hasStatus(HttpStatus.CONFLICT)
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.returns(release.id(), ServiceRelease::id)
				.returns(ReleaseState.FAILED, ServiceRelease::state)
				.satisfies(completed -> assertThat(completed.errors())
						.containsExactly("Artifact with coordinates '%s' was not uploaded"
								.formatted(ArtifactCoordinates.of(konfigyrArtifactoryArtifact).format()))
				);

		assertThat(context.select(SERVICE_RELEASES.STATE)
				.from(SERVICE_RELEASES)
				.where(SERVICE_RELEASES.ID.eq(EntityId.from(release.id()).get()))
				.fetchOne(SERVICE_RELEASES.STATE))
				.isEqualTo(ReleaseState.FAILED.name());
	}

	@Test
	@Transactional
	@DisplayName("should fail to complete a release that is not pending")
	void shouldRejectCompleteForNotPendingRelease() {
		final var metadata = metadata(konfigyrArtifactoryArtifact, "checksum");
		final var release = assertThatRelease(ServiceReleaseCandidate.of(metadata))
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.actual();

		assertThatUpload(release.id(), metadata).hasStatus(HttpStatus.NO_CONTENT);
		assertThatComplete(release.id()).hasStatusOk();

		assertThatComplete(release.id())
				.satisfies(problemDetailFor(HttpStatus.CONFLICT, problem -> problem
						.hasTitle("Release is not pending")
						.hasDetailContaining("Release is no longer pending")
				));
	}

	@Test
	@DisplayName("should fail to complete an unknown release")
	void shouldRejectCompleteForUnknownRelease() {
		assertThatComplete(EntityId.from(999).serialize())
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle("Release not found")
						.hasDetailContaining("Could not find a release with the following identifier")
				));
	}

	@Test
	@DisplayName("should fail to complete a release for an unknown service")
	void shouldRejectCompleteForUnknownService() {
		mvc.post().uri("/namespaces/konfigyr/services/unknown-service/releases/{id}/complete", EntityId.from(999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@Transactional
	@DisplayName("should retrieve a pending release with per-artifact upload status")
	void shouldRetrievePendingRelease() {
		final var uploaded = metadata(konfigyrArtifactoryArtifact, "checksum");
		final var release = assertThatRelease(List.of(
				ServiceReleaseCandidate.of(uploaded),
				ServiceReleaseCandidate.of(konfigyrCryptoApiArtifact, "checksum")
		)).actual();

		assertThatUpload(release.id(), uploaded).hasStatus(HttpStatus.NO_CONTENT);

		assertThatRelease(release.id())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.returns(release.id(), ServiceRelease::id)
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.returns(List.of(), ServiceRelease::errors)
				.satisfies(retrieved -> assertThat(retrieved.artifacts())
						.hasSize(2)
						.satisfiesExactlyInAnyOrder(
								entry -> assertThat(entry)
										.returns(ArtifactCoordinates.of(konfigyrArtifactoryArtifact), ArtifactCoordinates::of)
										.returns(ArtifactUploadStatus.SKIP, ServiceReleaseEntry::status),
								entry -> assertThat(entry)
										.returns(ArtifactCoordinates.of(konfigyrCryptoApiArtifact), ArtifactCoordinates::of)
										.returns(ArtifactUploadStatus.UPLOAD_REQUIRED, ServiceReleaseEntry::status)
						)
				);
	}

	@Test
	@Transactional
	@DisplayName("should retrieve a released release")
	void shouldRetrieveReleasedRelease() {
		final var metadata = metadata(konfigyrArtifactoryArtifact, "checksum");
		final var release = assertThatRelease(ServiceReleaseCandidate.of(metadata)).actual();

		assertThatUpload(release.id(), metadata).hasStatus(HttpStatus.NO_CONTENT);
		assertThatComplete(release.id()).hasStatusOk();

		assertThatRelease(release.id())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.returns(release.id(), ServiceRelease::id)
				.returns(ReleaseState.RELEASED, ServiceRelease::state)
				.returns(List.of(), ServiceRelease::errors)
				.satisfies(retrieved -> assertThat(retrieved.publishedAt()).isNotNull());
	}

	@Test
	@Transactional
	@DisplayName("should retrieve a failed release with the same errors reported by complete")
	void shouldRetrieveFailedRelease() {
		final var release = assertThatRelease(ServiceReleaseCandidate.of(konfigyrArtifactoryArtifact, "checksum")).actual();

		assertThatComplete(release.id()).hasStatus(HttpStatus.CONFLICT);

		assertThatRelease(release.id())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.returns(release.id(), ServiceRelease::id)
				.returns(ReleaseState.FAILED, ServiceRelease::state)
				.satisfies(retrieved -> assertThat(retrieved.errors())
						.containsExactly("Artifact with coordinates '%s' was not uploaded"
								.formatted(ArtifactCoordinates.of(konfigyrArtifactoryArtifact).format()))
				);
	}

	@Test
	@DisplayName("should fail to retrieve a release that does not exist")
	void shouldRejectRetrieveForUnknownRelease() {
		assertThatRelease(EntityId.from(999).serialize())
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle("Release not found")
						.hasDetailContaining("Could not find a release with the following identifier")
				));
	}

	@Test
	@DisplayName("should fail to retrieve a release for an unknown service")
	void shouldRejectRetrieveForUnknownService() {
		mvc.get().uri("/namespaces/konfigyr/services/unknown-service/releases/{id}", EntityId.from(999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not retrieve a release when user is not a member of the namespace")
	void shouldRejectRetrieveWhenNotAMember() {
		mvc.get().uri("/namespaces/john-doe/services/john-doe-blog/releases/{id}", EntityId.from(999).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.PUBLISH_MANIFESTS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not retrieve a release when the publish-manifests scope is not present")
	void shouldRejectRetrieveWithoutScope() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id/releases/{id}", EntityId.from(999).serialize())
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.PUBLISH_MANIFESTS));
	}

	@Test
	@DisplayName("should not resolve a release when user is not a member of the namespace")
	void shouldRejectWhenNotAMember() {
		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.jane(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content("[]")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not resolve a release when the publish-manifests scope is not present")
	void shouldRejectWithoutScope() {
		mvc.post().uri("/namespaces/konfigyr/services/konfigyr-id/releases")
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("[]")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.PUBLISH_MANIFESTS));
	}

	@Test
	@DisplayName("should fail to resolve a release for an unknown service")
	void shouldRejectUnknownService() {
		mvc.post().uri("/namespaces/konfigyr/services/unknown-service/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content("[]")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should fail to resolve a release for an unknown namespace")
	void shouldRejectUnknownNamespace() {
		mvc.post().uri("/namespaces/unknown-namespace/services/unknown-service/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content("[]")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	private ObjectAssert<ServiceRelease> assertThatRelease(ServiceReleaseCandidate... candidates) {
		return assertThatRelease(List.of(candidates));
	}

	private ObjectAssert<ServiceRelease> assertThatRelease(List<ServiceReleaseCandidate> candidates) {
		return mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(candidates))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.asInstanceOf(InstanceOfAssertFactories.type(ServiceRelease.class));
	}

	private MvcTestResultAssert assertThatRelease(String id) {
		return mvc.get().uri("/namespaces/john-doe/services/john-doe-blog/releases/{id}", id)
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.exchange()
				.assertThat()
				.apply(log());
	}

	private MvcTestResultAssert assertThatUpload(String id, ArtifactMetadata metadata) {
		return mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases/{id}/artifacts", id)
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(metadata))
				.exchange()
				.assertThat()
				.apply(log());
	}

	private MvcTestResultAssert assertThatComplete(String id) {
		return mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases/{id}/complete", id)
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.exchange()
				.assertThat()
				.apply(log());
	}

	private static ArtifactMetadata metadata(Artifact artifact, String checksum) {
		return ArtifactMetadata.builder()
				.groupId(artifact.groupId())
				.artifactId(artifact.artifactId())
				.version(artifact.version())
				.checksum(checksum)
				.property(PropertyDescriptor.builder()
						.name("some.property")
						.typeName("java.lang.String")
						.schema(StringSchema.instance())
						.build())
				.build();
	}

}
