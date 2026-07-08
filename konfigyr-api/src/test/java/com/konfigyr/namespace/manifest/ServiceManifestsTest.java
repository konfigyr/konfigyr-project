package com.konfigyr.namespace.manifest;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.namespace.Services;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceConfigurationCatalog.SERVICE_CONFIGURATION_CATALOG;
import static com.konfigyr.data.tables.ServiceReleases.SERVICE_RELEASES;
import static org.assertj.core.api.Assertions.*;

class ServiceManifestsTest extends AbstractIntegrationTest {

	static Artifact konfigyrArtifactoryArtifact = Artifact.of("com.konfigyr", "konfigyr-artifactory", "1.2.0");

	@Autowired
	ServiceManifests manifests;

	@Autowired
	NamespaceManager namespaces;

	@Autowired
	Services services;

	@Autowired
	DSLContext context;

	Service service;

	@BeforeEach
	void setUp() {
		final Namespace namespace = namespaces.findBySlug("konfigyr").orElseThrow();
		service = services.get(namespace, "konfigyr-id").orElseThrow();
	}

	@Test
	@Transactional
	@DisplayName("should open a release and upload artifact metadata for a declared coordinate and complete")
	void shouldProcessRelease(AssertablePublishedEvents events) {
		final var metadata = metadata(konfigyrArtifactoryArtifact);
		final var release = manifests.open(service, List.of(ServiceReleaseCandidate.of(metadata)));

		assertThat(release)
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.returns(List.of(), ServiceRelease::errors)
				.returns(null, ServiceRelease::publishedAt);

		assertThat(release.artifacts())
				.hasSize(1)
				.first()
				.returns(ArtifactUploadStatus.UPLOAD_REQUIRED, ServiceReleaseEntry::status);

		manifests.upload(service, EntityId.from(release.id()), metadata);

		assertThat(context.select(SERVICE_ARTIFACTS.SOURCE, SERVICE_ARTIFACTS.CHECKSUM)
				.from(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.GROUP_ID.eq(metadata.groupId()))
				.and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq(metadata.artifactId()))
				.and(SERVICE_ARTIFACTS.VERSION.eq(metadata.version()))
				.fetchOneMap())
				.containsEntry(SERVICE_ARTIFACTS.SOURCE.getName(), ArtifactSource.LOCAL.name())
				.containsEntry(SERVICE_ARTIFACTS.CHECKSUM.getName(), metadata.checksum());

		assertThat(context.fetchCount(SERVICE_CONFIGURATION_CATALOG,
				SERVICE_CONFIGURATION_CATALOG.GROUP_ID.eq(metadata.groupId())
						.and(SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID.eq(metadata.artifactId()))
						.and(SERVICE_CONFIGURATION_CATALOG.VERSION.eq(metadata.version()))
		)).isEqualTo(metadata.properties().size());

		final var completed = manifests.complete(service, EntityId.from(release.id()));

		assertThat(completed)
				.returns(ReleaseState.RELEASED, ServiceRelease::state)
				.returns(List.of(), ServiceRelease::errors);

		assertThat(completed.publishedAt())
				.isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));

		assertThat(context.select(SERVICE_RELEASES.STATE)
				.from(SERVICE_RELEASES)
				.where(SERVICE_RELEASES.ID.eq(EntityId.from(release.id()).get()))
				.fetchOne(SERVICE_RELEASES.STATE))
				.isEqualTo(ReleaseState.RELEASED.name());

		events.assertThat()
				.contains(ServiceEvent.Released.class)
				.matching(ServiceEvent.Released::get, service);
	}

	@Test
	@Transactional
	@DisplayName("should fail to upload artifact metadata for a coordinate that was not declared for the release")
	void shouldRejectUploadForUndeclaredCoordinate() {
		final var declared = metadata(konfigyrArtifactoryArtifact);
		final var release = manifests.open(service, List.of(ServiceReleaseCandidate.of(declared)));

		final var undeclared = metadata(Artifact.of("com.konfigyr", "konfigyr-crypto-api", "1.1.0"));

		assertThatExceptionOfType(UndeclaredArtifactException.class)
				.isThrownBy(() -> manifests.upload(service, EntityId.from(release.id()), undeclared))
				.returns(ArtifactCoordinates.of(undeclared), UndeclaredArtifactException::getCoordinates);
	}

	@Test
	@Transactional
	@DisplayName("should fail to upload artifact metadata when the release is no longer pending")
	void shouldRejectUploadForNotPendingRelease() {
		final var metadata = metadata(konfigyrArtifactoryArtifact);
		final var release = manifests.open(service, List.of(ServiceReleaseCandidate.of(metadata)));

		context.update(SERVICE_RELEASES)
				.set(SERVICE_RELEASES.STATE, ReleaseState.RELEASED.name())
				.where(SERVICE_RELEASES.ID.eq(EntityId.from(release.id()).get()))
				.execute();

		assertThatExceptionOfType(ReleaseNotPendingException.class)
				.isThrownBy(() -> manifests.upload(service, EntityId.from(release.id()), metadata))
				.returns(ReleaseState.RELEASED, ReleaseNotPendingException::getState)
				.returns(EntityId.from(release.id()), ReleaseNotPendingException::getReleaseId);
	}

	@Test
	@Transactional
	@DisplayName("should fail to complete a release when a declared artifact was never uploaded")
	void shouldFailToCompleteReleaseWithMissingUpload(AssertablePublishedEvents events) {
		final var metadata = metadata(konfigyrArtifactoryArtifact);
		final var release = manifests.open(service, List.of(ServiceReleaseCandidate.of(metadata)));

		final var completed = manifests.complete(service, EntityId.from(release.id()));

		assertThat(completed.state()).isEqualTo(ReleaseState.FAILED);
		assertThat(completed.errors()).containsExactly(
				"Artifact with coordinates '%s' was not uploaded".formatted(ArtifactCoordinates.of(metadata).format())
		);

		assertThat(context.select(SERVICE_RELEASES.STATE)
				.from(SERVICE_RELEASES)
				.where(SERVICE_RELEASES.ID.eq(EntityId.from(release.id()).get()))
				.fetchOne(SERVICE_RELEASES.STATE))
				.isEqualTo(ReleaseState.FAILED.name());

		events.assertThat()
				.contains(ServiceEvent.ReleaseFailed.class)
				.matching(ServiceEvent.ReleaseFailed::errors, completed.errors());
	}

	@Test
	@DisplayName("should fail to complete a release that does not exist")
	void shouldRejectCompleteForUnknownRelease() {
		assertThatExceptionOfType(ReleaseNotFoundException.class)
				.isThrownBy(() -> manifests.complete(service, EntityId.from(95673678)))
				.returns(EntityId.from(95673678), ReleaseNotFoundException::getReleaseId);
	}

	@Test
	@Transactional
	@DisplayName("should fail to complete a release that is not pending")
	void shouldRejectCompleteForNotPendingRelease() {
		final var metadata = metadata(konfigyrArtifactoryArtifact);
		final var release = manifests.open(service, List.of(ServiceReleaseCandidate.of(metadata)));
		manifests.upload(service, EntityId.from(release.id()), metadata);
		manifests.complete(service, EntityId.from(release.id()));

		assertThatExceptionOfType(ReleaseNotPendingException.class)
				.isThrownBy(() -> manifests.complete(service, EntityId.from(release.id())))
				.returns(ReleaseState.RELEASED, ReleaseNotPendingException::getState);
	}

	@Test
	@Transactional
	@DisplayName("should retrieve a pending release with per-artifact upload status")
	void shouldRetrievePendingRelease() {
		final var uploaded = metadata(konfigyrArtifactoryArtifact);
		final var pending = metadata(Artifact.of("com.konfigyr", "konfigyr-crypto-api", "1.1.0"));

		final var release = manifests.open(service, List.of(
				ServiceReleaseCandidate.of(uploaded),
				ServiceReleaseCandidate.of(pending)
		));

		manifests.upload(service, EntityId.from(release.id()), uploaded);

		assertThat(manifests.get(service, EntityId.from(release.id())))
				.isPresent()
				.get()
				.returns(release.id(), ServiceRelease::id)
				.returns(ReleaseState.PENDING, ServiceRelease::state)
				.returns(null, ServiceRelease::publishedAt)
				.returns(List.of(), ServiceRelease::errors)
				.extracting(ServiceRelease::artifacts)
				.satisfies(entries -> assertThat(entries)
						.hasSize(2)
						.satisfiesExactlyInAnyOrder(
								entry -> assertThat(entry)
										.returns(ArtifactCoordinates.of(uploaded), ArtifactCoordinates::of)
										.returns(ArtifactUploadStatus.SKIP, ServiceReleaseEntry::status),
								entry -> assertThat(entry)
										.returns(ArtifactCoordinates.of(pending), ArtifactCoordinates::of)
										.returns(ArtifactUploadStatus.UPLOAD_REQUIRED, ServiceReleaseEntry::status)
						)
				);
	}

	@Test
	@Transactional
	@DisplayName("should retrieve a released release")
	void shouldRetrieveReleasedRelease() {
		final var metadata = metadata(konfigyrArtifactoryArtifact);
		final var release = manifests.open(service, List.of(ServiceReleaseCandidate.of(metadata)));

		manifests.upload(service, EntityId.from(release.id()), metadata);
		final var completed = manifests.complete(service, EntityId.from(release.id()));

		assertThat(manifests.get(service, EntityId.from(release.id())))
				.isPresent()
				.get()
				.returns(release.id(), ServiceRelease::id)
				.returns(ReleaseState.RELEASED, ServiceRelease::state)
				.returns(List.of(), ServiceRelease::errors)
				.extracting(ServiceRelease::publishedAt, InstanceOfAssertFactories.INSTANT)
				.isCloseTo(completed.publishedAt(), within(1, ChronoUnit.SECONDS));
	}

	@Test
	@Transactional
	@DisplayName("should retrieve a failed release with the same errors reported by complete")
	void shouldRetrieveFailedRelease() {
		final var metadata = metadata(konfigyrArtifactoryArtifact);
		final var release = manifests.open(service, List.of(ServiceReleaseCandidate.of(metadata)));

		final var completed = manifests.complete(service, EntityId.from(release.id()));

		assertThat(manifests.get(service, EntityId.from(release.id())))
				.isPresent()
				.get()
				.returns(release.id(), ServiceRelease::id)
				.returns(ReleaseState.FAILED, ServiceRelease::state)
				.returns(completed.errors(), ServiceRelease::errors)
				.returns(null, ServiceRelease::publishedAt);
	}

	@Test
	@DisplayName("should fail to retrieve a release that does not exist")
	void shouldNotRetrieveUnknownRelease() {
		assertThat(manifests.get(service, EntityId.from(95673678))).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should fail to retrieve a release belonging to a different service")
	void shouldNotRetrieveReleaseForDifferentService() {
		final var metadata = metadata(konfigyrArtifactoryArtifact);
		final var release = manifests.open(service, List.of(ServiceReleaseCandidate.of(metadata)));

		final Namespace namespace = namespaces.findBySlug("konfigyr").orElseThrow();
		final Service otherService = services.get(namespace, "konfigyr-api").orElseThrow();

		assertThat(manifests.get(otherService, EntityId.from(release.id()))).isEmpty();
	}

	private static ArtifactMetadata metadata(Artifact artifact) {
		return artifact.toMetadata(List.of(
				PropertyDescriptor.builder()
						.name("some.property")
						.typeName("java.lang.String")
						.schema(StringSchema.instance())
						.build()
		));
	}

}
