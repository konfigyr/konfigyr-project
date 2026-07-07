package com.konfigyr.namespace.manifest;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.test.AbstractIntegrationTest;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceConfigurationCatalog.SERVICE_CONFIGURATION_CATALOG;
import static com.konfigyr.data.tables.ServiceReleases.SERVICE_RELEASES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
	@DisplayName("should open a release and upload artifact metadata for a declared coordinate")
	void shouldOpenAndUploadArtifactMetadata() {
		final var metadata = metadata(konfigyrArtifactoryArtifact);
		final var release = manifests.open(service, List.of(ServiceReleaseCandidate.of(metadata)));

		assertThat(release.state())
				.isEqualTo(ReleaseState.PENDING);

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
