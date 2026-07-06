package com.konfigyr.namespace.controller;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.ArtifactMetadata;
import com.konfigyr.artifactory.ArtifactSource;
import com.konfigyr.artifactory.ArtifactUploadStatus;
import com.konfigyr.artifactory.PropertyDescriptor;
import com.konfigyr.artifactory.ReleaseState;
import com.konfigyr.artifactory.ServiceRelease;
import com.konfigyr.artifactory.ServiceReleaseCandidate;
import com.konfigyr.artifactory.StringSchema;
import com.konfigyr.artifactory.TestArtifacts;
import com.konfigyr.entity.EntityId;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceConfigurationCatalog.SERVICE_CONFIGURATION_CATALOG;
import static com.konfigyr.data.tables.ServiceReleases.SERVICE_RELEASES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class ServiceManifestControllerTest extends AbstractControllerTest {

	@Autowired
	DSLContext context;

	@Test
	@Transactional
	@DisplayName("should open a new release when the service has none")
	void shouldOpenNewRelease() {
		final var request = new ServiceManifestController.ResolveReleaseRequest(List.of(
				ServiceReleaseCandidate.of("com.acme", "my-service", "1.2.0", "checksum-1"),
				ServiceReleaseCandidate.of("com.acme", "other-service", "2.0.0", "checksum-2")
		));

		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.returns(com.konfigyr.artifactory.ReleaseState.PENDING, ServiceRelease::state)
				.satisfies(release -> assertThat(release.artifacts())
						.hasSize(2)
						.allSatisfy(entry -> assertThat(entry.status()).isEqualTo(ArtifactUploadStatus.UPLOAD_REQUIRED))
				);
	}

	@Test
	@Transactional
	@DisplayName("should skip an artifact that is already indexed in the Artifactory")
	void shouldSkipIndexedArtifact() {
		final var request = new ServiceManifestController.ResolveReleaseRequest(List.of(
				ServiceReleaseCandidate.of("com.konfigyr", "konfigyr-crypto-api", "1.0.0", "irrelevant-checksum")
		));

		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.satisfies(release -> assertThat(release.artifacts())
						.hasSize(1)
						.first()
						.returns(ArtifactUploadStatus.SKIP, entry -> entry.status())
				);

		assertThat(context.select(SERVICE_ARTIFACTS.SOURCE, SERVICE_ARTIFACTS.CHECKSUM)
				.from(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.GROUP_ID.eq("com.konfigyr"))
				.and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq("konfigyr-crypto-api"))
				.and(SERVICE_ARTIFACTS.VERSION.eq("1.0.0"))
				.fetchOne())
				.satisfies(record -> {
					assertThat(record.get(SERVICE_ARTIFACTS.SOURCE)).isEqualTo(ArtifactSource.ARTIFACTORY.name());
					assertThat(record.get(SERVICE_ARTIFACTS.CHECKSUM)).isNull();
				});
	}

	@Test
	@Transactional
	@DisplayName("should take over an existing release and skip artifacts with a matching checksum")
	void shouldTakeOverExistingRelease() {
		final var request = new ServiceManifestController.ResolveReleaseRequest(List.of(
				ServiceReleaseCandidate.of("com.acme", "my-service", "1.2.0", "checksum-1")
		));

		final ServiceRelease first = mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(request))
				.exchange()
				.assertThat()
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.actual();

		// simulate T8's upload() having already recorded this checksum for the artifact
		markUploaded("com.acme", "my-service", "1.2.0", "checksum-1");

		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(request))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.returns(first.id(), ServiceRelease::id)
				.satisfies(release -> assertThat(release.artifacts())
						.hasSize(1)
						.first()
						.returns(ArtifactUploadStatus.SKIP, entry -> entry.status())
				);
	}

	@Test
	@Transactional
	@DisplayName("should mark an artifact upload required when its checksum changed")
	void shouldRequireUploadWhenChecksumChanged() {
		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(new ServiceManifestController.ResolveReleaseRequest(List.of(
						ServiceReleaseCandidate.of("com.acme", "my-service", "1.2.0", "checksum-1")
				))))
				.exchange()
				.assertThat()
				.hasStatusOk();

		// simulate T8's upload() having already recorded the old checksum for the artifact
		markUploaded("com.acme", "my-service", "1.2.0", "checksum-1");

		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(new ServiceManifestController.ResolveReleaseRequest(List.of(
						ServiceReleaseCandidate.of("com.acme", "my-service", "1.2.0", "checksum-2")
				))))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.satisfies(release -> assertThat(release.artifacts())
						.hasSize(1)
						.first()
						.returns(ArtifactUploadStatus.UPLOAD_REQUIRED, entry -> entry.status())
				);
	}

	private void markUploaded(String groupId, String artifactId, String version, String checksum) {
		final int updated = context.update(SERVICE_ARTIFACTS)
				.set(SERVICE_ARTIFACTS.CHECKSUM, checksum)
				.where(SERVICE_ARTIFACTS.GROUP_ID.eq(groupId))
				.and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq(artifactId))
				.and(SERVICE_ARTIFACTS.VERSION.eq(version))
				.execute();

		assertThat(updated).as("Expected an existing service_artifacts row to update").isOne();
	}

	@Test
	@Transactional
	@DisplayName("should prune artifacts that are no longer declared")
	void shouldPruneStaleArtifacts() {
		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(new ServiceManifestController.ResolveReleaseRequest(List.of(
						ServiceReleaseCandidate.of("com.acme", "my-service", "1.2.0", "checksum-1"),
						ServiceReleaseCandidate.of("com.acme", "other-service", "2.0.0", "checksum-2")
				))))
				.exchange()
				.assertThat()
				.hasStatusOk();

		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(new ServiceManifestController.ResolveReleaseRequest(List.of(
						ServiceReleaseCandidate.of("com.acme", "my-service", "1.2.0", "checksum-1")
				))))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.satisfies(release -> assertThat(release.artifacts()).hasSize(1));

		assertThat(context.fetchExists(SERVICE_ARTIFACTS,
				SERVICE_ARTIFACTS.GROUP_ID.eq("com.acme").and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq("other-service"))
		)).isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should upload artifact metadata for a declared coordinate")
	void shouldUploadArtifactMetadata() {
		final var coordinates = ArtifactCoordinates.of("com.acme", "my-service", "1.2.0");
		final String id = openRelease(coordinates);
		final ArtifactMetadata metadata = TestArtifacts.metadata(coordinates);

		uploadArtifact(id, metadata)
				.assertThat()
				.apply(log())
				.hasStatusOk();

		assertThat(context.select(SERVICE_ARTIFACTS.SOURCE, SERVICE_ARTIFACTS.CHECKSUM)
				.from(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.GROUP_ID.eq(coordinates.groupId()))
				.and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq(coordinates.artifactId()))
				.and(SERVICE_ARTIFACTS.VERSION.eq(coordinates.version().get()))
				.fetchOne())
				.satisfies(record -> {
					assertThat(record.get(SERVICE_ARTIFACTS.SOURCE)).isEqualTo(ArtifactSource.LOCAL.name());
					assertThat(record.get(SERVICE_ARTIFACTS.CHECKSUM)).isEqualTo(metadata.checksum());
				});

		assertThat(context.select(SERVICE_CONFIGURATION_CATALOG.NAME, SERVICE_CONFIGURATION_CATALOG.SEARCH_VECTOR)
				.from(SERVICE_CONFIGURATION_CATALOG)
				.where(SERVICE_CONFIGURATION_CATALOG.GROUP_ID.eq(coordinates.groupId()))
				.and(SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID.eq(coordinates.artifactId()))
				.and(SERVICE_CONFIGURATION_CATALOG.VERSION.eq(coordinates.version().get()))
				.fetch())
				.hasSize(metadata.properties().size())
				.allSatisfy(record -> assertThat(record.get(SERVICE_CONFIGURATION_CATALOG.SEARCH_VECTOR)).isNotNull())
				.extracting(record -> record.get(SERVICE_CONFIGURATION_CATALOG.NAME))
				.containsExactlyInAnyOrderElementsOf(metadata.properties().stream().map(PropertyDescriptor::name).toList());
	}

	@Test
	@Transactional
	@DisplayName("should replace catalog rows and update the checksum when the same coordinate is uploaded again")
	void shouldReplaceCatalogRowsOnReupload() {
		final var coordinates = ArtifactCoordinates.of("com.acme", "my-service", "1.2.0");
		final String id = openRelease(coordinates);

		uploadArtifact(id, TestArtifacts.metadata(coordinates)).assertThat().hasStatusOk();

		final ArtifactMetadata metadata = TestArtifacts.metadata(coordinates, PropertyDescriptor.builder()
				.name("com.acme.my-service.enabled")
				.typeName("java.lang.Boolean")
				.schema(StringSchema.instance())
				.build());

		uploadArtifact(id, metadata)
				.assertThat()
				.apply(log())
				.hasStatusOk();

		assertThat(context.select(SERVICE_CONFIGURATION_CATALOG.NAME)
				.from(SERVICE_CONFIGURATION_CATALOG)
				.where(SERVICE_CONFIGURATION_CATALOG.GROUP_ID.eq(coordinates.groupId()))
				.and(SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID.eq(coordinates.artifactId()))
				.and(SERVICE_CONFIGURATION_CATALOG.VERSION.eq(coordinates.version().get()))
				.fetch(SERVICE_CONFIGURATION_CATALOG.NAME))
				.containsExactly("com.acme.my-service.enabled");

		assertThat(context.select(SERVICE_ARTIFACTS.CHECKSUM)
				.from(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.GROUP_ID.eq(coordinates.groupId()))
				.and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq(coordinates.artifactId()))
				.and(SERVICE_ARTIFACTS.VERSION.eq(coordinates.version().get()))
				.fetchOne(SERVICE_ARTIFACTS.CHECKSUM))
				.isEqualTo(metadata.checksum());
	}

	@Test
	@Transactional
	@DisplayName("should reject an artifact upload when the release is not pending")
	void shouldRejectUploadForNotPendingRelease() {
		final var coordinates = ArtifactCoordinates.of("com.acme", "my-service", "1.2.0");
		final String id = openRelease(coordinates);

		final int updated = context.update(SERVICE_RELEASES)
				.set(SERVICE_RELEASES.STATE, ReleaseState.RELEASED.name())
				.where(SERVICE_RELEASES.ID.eq(EntityId.from(id).get()))
				.execute();

		assertThat(updated).as("Expected an existing service_releases row to update").isOne();

		uploadArtifact(id, TestArtifacts.metadata(coordinates))
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.CONFLICT, problem -> problem
						.hasTitle("Release is not pending")
						.hasDetailContaining("Release is no longer pending")
				));
	}

	@Test
	@Transactional
	@DisplayName("should reject an artifact upload for a coordinate that was not declared for the release")
	void shouldRejectUploadForUndeclaredCoordinate() {
		final String id = openRelease(ArtifactCoordinates.of("com.acme", "my-service", "1.2.0"));

		uploadArtifact(id, TestArtifacts.metadata(ArtifactCoordinates.of("com.acme", "other-service", "1.0.0")))
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle("Artifact not declared for this release")
						.hasDetailContaining("com.acme:other-service:1.0.0")
				));
	}

	@Test
	@Transactional
	@DisplayName("should reject a malformed artifact metadata payload")
	void shouldRejectMalformedUploadPayload() {
		final String id = openRelease(ArtifactCoordinates.of("com.acme", "my-service", "1.2.0"));

		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases/{id}/artifacts", id)
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not-valid-json")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	private String openRelease(ArtifactCoordinates coordinates) {
		return mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(new ServiceManifestController.ResolveReleaseRequest(List.of(
						ServiceReleaseCandidate.of(coordinates.groupId(), coordinates.artifactId(), coordinates.version().get(), "irrelevant-checksum")
				))))
				.exchange()
				.assertThat()
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceRelease.class)
				.actual()
				.id();
	}

	private MvcTestResult uploadArtifact(String id, ArtifactMetadata metadata) {
		return mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases/{id}/artifacts", id)
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(metadata))
				.exchange();
	}

	@Test
	@DisplayName("should not resolve a release when user is not a member of the namespace")
	void shouldRejectWhenNotAMember() {
		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/releases")
				.with(authentication(TestPrincipals.jane(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(new ServiceManifestController.ResolveReleaseRequest(List.of())))
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
				.content(jsonMapper.writeValueAsBytes(new ServiceManifestController.ResolveReleaseRequest(List.of())))
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
				.content(jsonMapper.writeValueAsBytes(new ServiceManifestController.ResolveReleaseRequest(List.of())))
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
				.content(jsonMapper.writeValueAsBytes(new ServiceManifestController.ResolveReleaseRequest(List.of())))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

}
