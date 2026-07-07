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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceConfigurationCatalog.SERVICE_CONFIGURATION_CATALOG;
import static com.konfigyr.data.tables.ServiceReleases.SERVICE_RELEASES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class ServiceManifestControllerTest extends AbstractControllerTest {

	static Artifact konfigyrArtifactoryArtifact = Artifact.of("com.konfigyr", "konfigyr-artifactory", "1.2.0");
	static Artifact konfigyrCryptoApiArtifact = Artifact.of("com.konfigyr", "konfigyr-crypto-api", "1.1.0");

	@Autowired
	DSLContext context;

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
				.hasStatusOk();

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
				.hasStatusOk();

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
				.hasStatusOk();

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
				.hasStatusOk();

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

	private MvcTestResultAssert assertThatUpload(String id, ArtifactMetadata metadata) {
		return mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases/{id}/artifacts", id)
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(metadata))
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
