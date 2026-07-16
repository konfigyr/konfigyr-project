package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceCatalog;
import com.konfigyr.namespace.Services;
import com.konfigyr.namespace.manifest.ServiceManifests;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;

import static com.konfigyr.data.tables.ArtifactVersionProperties.ARTIFACT_VERSION_PROPERTIES;
import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.PropertyDefinitions.PROPERTY_DEFINITIONS;
import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static org.assertj.core.api.Assertions.*;

class ServiceCatalogWorkerTest extends AbstractIntegrationTest {

	@Autowired
	Services services;

	@Autowired
	ServiceManifests manifests;

	@Autowired
	ServiceCatalogWorker worker;

	@Autowired
	DSLContext context;

	@Autowired
	JsonMapper jsonMapper;

	@Test
	@DisplayName("should fail to build service catalog for unknown service release")
	void buildForUnknownRelease() {
		assertThatIllegalStateException()
				.isThrownBy(() -> worker.build(EntityId.from(9999)))
				.withMessageContaining("Failed to resolve service release with identifier: %s", EntityId.from(9999))
				.withNoCause();
	}

	@Test
	@Transactional
	@DisplayName("should build the service catalog for a service release with matching property descriptors")
	void buildServiceCatalog() {
		final var service = serviceFor(3);

		assertThatNoException().isThrownBy(() -> worker.build(EntityId.from(2)));

		assertThatObject(manifests.get(service))
				.returns(EntityId.from(2).serialize(), Manifest::id)
				.returns(service.name(), Manifest::name)
				.extracting(Manifest::artifacts, InstanceOfAssertFactories.iterable(ManifestEntry.class))
				.hasSize(4)
				.extracting(ArtifactCoordinates::of, ManifestEntry::source, ManifestEntry::checksum)
				.containsExactlyInAnyOrder(
						tuple(
								ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.1"),
								ArtifactSource.ARTIFACTORY,
								"b4114c17ac46992508628d6c905e39b73a0208df56374dba3ddc545016f83fb7"
						),
						tuple(
								ArtifactCoordinates.parse("org.springframework.modulith:spring-modulith-core:2.0.4"),
								ArtifactSource.ARTIFACTORY,
								"ec54eb43a2f17d3fecf5062c987c794ea025da258de0b6ea6483542ef79e3f8a"
						),
						tuple(
								ArtifactCoordinates.parse("org.springframework.boot:spring-boot:4.0.4"),
								ArtifactSource.ARTIFACTORY,
								"ec54eb43a2f17d3fecf5062c987c794ea025da258de0b6ea6483542ef79e3f8a"
						),
						tuple(
								ArtifactCoordinates.parse("com.acme:spring-boot-library:1.1.4"),
								ArtifactSource.LOCAL,
								"HX26naW4bSuS+0yUHCyXw83XsVgNAyafKDHD576DyhA="
						)
				);

		assertThat(services.catalog(service.id()))
				.hasSize(7)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("com.acme:spring-boot-library:1.1.4"),
										ServiceCatalog.Property::artifact)
								.returns("com.acme.library.deprecated", ServiceCatalog.Property::name)
								.returns("java.lang.Integer", ServiceCatalog.Property::typeName)
								.returns(IntegerSchema.instance(), ServiceCatalog.Property::schema)
								.returns("Deprecated local library property.", ServiceCatalog.Property::description)
								.returns("567889", ServiceCatalog.Property::defaultValue)
								.returns(new Deprecation("Removed without replacement", null), ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("com.acme:spring-boot-library:1.1.4"),
										ServiceCatalog.Property::artifact)
								.returns("com.acme.library.property", ServiceCatalog.Property::name)
								.returns("java.lang.Boolean", ServiceCatalog.Property::typeName)
								.returns(BooleanSchema.instance(), ServiceCatalog.Property::schema)
								.returns("Local library property.", ServiceCatalog.Property::description)
								.returns("true", ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.1"),
										ServiceCatalog.Property::artifact)
								.returns("spring.application.name", ServiceCatalog.Property::name)
								.returns("java.lang.String", ServiceCatalog.Property::typeName)
								.returns(StringSchema.instance(), ServiceCatalog.Property::schema)
								.returns("Application name. Typically used with logging to help identify the application.",
										ServiceCatalog.Property::description)
								.returns(null, ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.modulith:spring-modulith-core:2.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("spring.modulith.detection-strategy", ServiceCatalog.Property::name)
								.returns("java.lang.String", ServiceCatalog.Property::typeName)
								.returns(StringSchema.builder().example("direct-sub-packages").example("explicitly-annotated").build(),
										ServiceCatalog.Property::schema)
								.returns("The strategy how to detect application modules.",
										ServiceCatalog.Property::description)
								.returns(null, ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("spring.profiles.active", ServiceCatalog.Property::name)
								.returns("java.util.List<java.lang.String>", ServiceCatalog.Property::typeName)
								.returns(ArraySchema.of(StringSchema.instance()), ServiceCatalog.Property::schema)
								.returns("Comma-separated list of active profiles. Can be overridden by a command line switch.",
										ServiceCatalog.Property::description)
								.returns(null, ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("spring.profiles.default", ServiceCatalog.Property::name)
								.returns("java.lang.String", ServiceCatalog.Property::typeName)
								.returns(StringSchema.instance(), ServiceCatalog.Property::schema)
								.returns("Name of the profile to enable if no profile is active.",
										ServiceCatalog.Property::description)
								.returns("default", ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("spring.profiles.validate", ServiceCatalog.Property::name)
								.returns("java.lang.Boolean", ServiceCatalog.Property::typeName)
								.returns(BooleanSchema.instance(), ServiceCatalog.Property::schema)
								.returns("Whether profiles should be validated to ensure sensible names are used.",
										ServiceCatalog.Property::description)
								.returns("true", ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation)
				);
	}

	@Test
	@Transactional
	@DisplayName("should re-build the service catalog for a service release with matching property descriptors")
	void rebuildServiceCatalog() {
		final var service = serviceFor(2);

		assertThatNoException().isThrownBy(() -> worker.build(EntityId.from(1)));

		assertThatObject(manifests.get(service))
				.returns(EntityId.from(1).serialize(), Manifest::id)
				.returns(service.name(), Manifest::name)
				.extracting(Manifest::artifacts, InstanceOfAssertFactories.iterable(ManifestEntry.class))
				.hasSize(8);

		assertThat(services.catalog(service.id()))
				.hasSize(11)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("com.acme:spring-boot-service:1.0.0"),
										ServiceCatalog.Property::artifact)
								.returns("com.acme.service.property", ServiceCatalog.Property::name)
								.returns("java.lang.String", ServiceCatalog.Property::typeName)
								.returns(StringSchema.instance(), ServiceCatalog.Property::schema)
								.returns("Local service property.", ServiceCatalog.Property::description)
								.returns(null, ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot-actuator:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("management.endpoint.sbom.access", ServiceCatalog.Property::name)
								.returns("org.springframework.boot.actuate.endpoint.Access", ServiceCatalog.Property::typeName)
								.returns(StringSchema.builder().enumeration("NONE").enumeration("READ_ONLY").enumeration("UNRESTRICTED").build(),
										ServiceCatalog.Property::schema)
								.returns("Permitted level of access for the sbom endpoint.",
										ServiceCatalog.Property::description)
								.returns("unrestricted", ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot-actuator:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("management.endpoint.sbom.application.media-type", ServiceCatalog.Property::name)
								.returns("org.springframework.util.MimeType", ServiceCatalog.Property::typeName)
								.returns(StringSchema.builder().format("mime-type").build(), ServiceCatalog.Property::schema)
								.returns("Media type of the SBOM. If null, the media type will be auto-detected.",
										ServiceCatalog.Property::description)
								.returns(null, ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot-actuator:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("management.endpoint.shutdown.access", ServiceCatalog.Property::name)
								.returns("org.springframework.boot.actuate.endpoint.Access", ServiceCatalog.Property::typeName)
								.returns(StringSchema.builder().enumeration("NONE").enumeration("READ_ONLY").enumeration("UNRESTRICTED").build(),
										ServiceCatalog.Property::schema)
								.returns("Permitted level of access for the shutdown endpoint.",
										ServiceCatalog.Property::description)
								.returns("none", ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot-actuator:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("management.endpoint.startup.access", ServiceCatalog.Property::name)
								.returns("org.springframework.boot.actuate.endpoint.Access", ServiceCatalog.Property::typeName)
								.returns(StringSchema.builder().enumeration("NONE").enumeration("READ_ONLY").enumeration("UNRESTRICTED").build(),
										ServiceCatalog.Property::schema)
								.returns("Permitted level of access for the startup endpoint.",
										ServiceCatalog.Property::description)
								.returns("unrestricted", ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot-autoconfigure:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("spring.messages.basename", ServiceCatalog.Property::name)
								.returns("java.util.List<java.lang.String>", ServiceCatalog.Property::typeName)
								.returns(ArraySchema.of(StringSchema.instance()), ServiceCatalog.Property::schema)
								.returns("messages", ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot-autoconfigure:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("spring.messages.cache-duration", ServiceCatalog.Property::name)
								.returns("java.time.Duration", ServiceCatalog.Property::typeName)
								.returns(StringSchema.builder().format("duration").build(),
										ServiceCatalog.Property::schema)
								.returns(null, ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot-autoconfigure:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("spring.messages.encoding", ServiceCatalog.Property::name)
								.returns("java.nio.charset.Charset", ServiceCatalog.Property::typeName)
								.returns(StringSchema.builder().format("charset").build(),
										ServiceCatalog.Property::schema)
								.returns("Message bundles encoding.", ServiceCatalog.Property::description)
								.returns("UTF-8", ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("spring.profiles.active", ServiceCatalog.Property::name)
								.returns("java.util.List<java.lang.String>", ServiceCatalog.Property::typeName)
								.returns(ArraySchema.of(StringSchema.instance()), ServiceCatalog.Property::schema)
								.returns("Comma-separated list of active profiles. Can be overridden by a command line switch.",
										ServiceCatalog.Property::description)
								.returns(null, ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("spring.profiles.default", ServiceCatalog.Property::name)
								.returns("java.lang.String", ServiceCatalog.Property::typeName)
								.returns(StringSchema.instance(), ServiceCatalog.Property::schema)
								.returns("Name of the profile to enable if no profile is active.",
										ServiceCatalog.Property::description)
								.returns("default", ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation),
						it -> assertThat(it)
								.returns(ArtifactCoordinates.parse("org.springframework.boot:spring-boot:4.0.4"),
										ServiceCatalog.Property::artifact)
								.returns("spring.profiles.validate", ServiceCatalog.Property::name)
								.returns("java.lang.Boolean", ServiceCatalog.Property::typeName)
								.returns(BooleanSchema.instance(), ServiceCatalog.Property::schema)
								.returns("Whether profiles should be validated to ensure sensible names are used.",
										ServiceCatalog.Property::description)
								.returns("true", ServiceCatalog.Property::defaultValue)
								.returns(null, ServiceCatalog.Property::deprecation)
				);
	}

	@Test
	@Transactional
	@DisplayName("should promote a LOCAL artifact to ARTIFACTORY once it is indexed and rebuild its properties")
	void promoteLocalArtifactOnceIndexedByArtifactory() throws Exception {
		final var service = serviceFor(3);
		final var metadata = jsonMapper.readValue(
				new ClassPathResource("fixtures/com.acme:spring-boot-library:1.1.4.json").getInputStream(),
				ArtifactMetadata.class
		);

		assertThatNoException().isThrownBy(() -> insertArtifactMetadataPublication(metadata));
		assertThatNoException().isThrownBy(() -> worker.build(EntityId.from(2)));

		final var coordinates = ArtifactCoordinates.of(metadata);

		assertThat(context.selectFrom(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(2L))
				.and(SERVICE_ARTIFACTS.COORDINATES.eq(coordinates.format()))
				.fetchOne())
				.isNotNull()
				.returns(ArtifactSource.ARTIFACTORY.name(), SERVICE_ARTIFACTS.SOURCE::get)
				.returns(null, SERVICE_ARTIFACTS.CHECKSUM::get)
				.returns(null, SERVICE_ARTIFACTS.NAME::get)
				.returns(null, SERVICE_ARTIFACTS.DESCRIPTION::get)
				.returns(null, SERVICE_ARTIFACTS.WEBSITE::get)
				.returns(null, SERVICE_ARTIFACTS.REPOSITORY::get);

		assertThatObject(manifests.get(service))
				.returns(EntityId.from(2).serialize(), Manifest::id)
				.returns(service.name(), Manifest::name)
				.extracting(Manifest::artifacts, InstanceOfAssertFactories.iterable(ManifestEntry.class))
				.hasSize(4)
				.extracting(ArtifactCoordinates::of, ManifestEntry::source, ManifestEntry::checksum)
				.containsExactlyInAnyOrder(
						tuple(
								ArtifactCoordinates.parse("com.konfigyr:konfigyr-crypto-api:1.0.1"),
								ArtifactSource.ARTIFACTORY,
								"b4114c17ac46992508628d6c905e39b73a0208df56374dba3ddc545016f83fb7"
						),
						tuple(
								ArtifactCoordinates.parse("org.springframework.modulith:spring-modulith-core:2.0.4"),
								ArtifactSource.ARTIFACTORY,
								"ec54eb43a2f17d3fecf5062c987c794ea025da258de0b6ea6483542ef79e3f8a"
						),
						tuple(
								ArtifactCoordinates.parse("org.springframework.boot:spring-boot:4.0.4"),
								ArtifactSource.ARTIFACTORY,
								"ec54eb43a2f17d3fecf5062c987c794ea025da258de0b6ea6483542ef79e3f8a"
						),
						tuple(
								ArtifactCoordinates.parse("com.acme:spring-boot-library:1.1.4"),
								ArtifactSource.ARTIFACTORY,
								"0688b5581b22b2ab5740e6b7f6d5f72ca159e571d7c40994329c95a7e9427edd"
						)
				);

		assertThat(services.catalog(service.id()))
				.hasSize(7)
				.extracting(ServiceCatalog.Property::artifact, ServiceCatalog.Property::name)
				.contains(tuple(coordinates, "com.acme.library.imported"))
				.contains(tuple(coordinates, "com.acme.library.property"))
				.doesNotContain(tuple(coordinates, "com.acme.library.deprecated"));
	}

	Service serviceFor(long id) {
		return assertThat(services.get(EntityId.from(id)))
				.as("Service with id %s must be present", id)
				.isPresent()
				.get()
				.actual();
	}

	private void insertArtifactMetadataPublication(ArtifactMetadata metadata) {
		final Long artifactId = context.insertInto(ARTIFACTS)
				.set(ARTIFACTS.NAMESPACE_ID, 2L)
				.set(ARTIFACTS.GROUP_ID, metadata.groupId())
				.set(ARTIFACTS.ARTIFACT_ID, metadata.artifactId())
				.set(ARTIFACTS.VISIBILITY, ArtifactVisibility.PRIVATE.name())
				.set(ARTIFACTS.NAME, metadata.name())
				.set(ARTIFACTS.DESCRIPTION, metadata.description())
				.set(ARTIFACTS.WEBSITE, Objects.toString(metadata.website(), null))
				.set(ARTIFACTS.REPOSITORY, Objects.toString(metadata.repository(), null))
				.returning(ARTIFACTS.ID)
				.fetchOne(ARTIFACTS.ID);

		final Long artifactVersionId = context.insertInto(ARTIFACT_VERSIONS)
				.set(ARTIFACT_VERSIONS.ARTIFACT_ID, artifactId)
				.set(ARTIFACT_VERSIONS.VERSION, metadata.version())
				.set(ARTIFACT_VERSIONS.STATE, PublicationState.PUBLISHED.name())
				.set(ARTIFACT_VERSIONS.CHECKSUM, ByteArray.fromBase64String(metadata.checksum()))
				.returning(ARTIFACT_VERSIONS.ID)
				.fetchOne(ARTIFACT_VERSIONS.ID);

		var propertyInsertQuery = context.insertInto(PROPERTY_DEFINITIONS).columns(
				PROPERTY_DEFINITIONS.ARTIFACT_ID,
				PROPERTY_DEFINITIONS.CHECKSUM,
				PROPERTY_DEFINITIONS.NAME,
				PROPERTY_DEFINITIONS.TYPE_NAME,
				PROPERTY_DEFINITIONS.SCHEMA,
				PROPERTY_DEFINITIONS.DESCRIPTION
		);

		for (final var property : metadata.properties()) {
			propertyInsertQuery = propertyInsertQuery.values(
					artifactId,
					ByteArray.fromString(property.name()),
					property.name(),
					property.typeName(),
					ByteArray.fromString(jsonMapper.writeValueAsString(property.schema())),
					property.description()
			);
		}

		final List<Long> propertyDefinitions = propertyInsertQuery
				.returning(PROPERTY_DEFINITIONS.ID)
				.fetch(PROPERTY_DEFINITIONS.ID);

		var propertyVersionInsertQuery = context.insertInto(ARTIFACT_VERSION_PROPERTIES).columns(
				ARTIFACT_VERSION_PROPERTIES.ARTIFACT_VERSION_ID,
				ARTIFACT_VERSION_PROPERTIES.PROPERTY_DEFINITION_ID
		);

		for (final var id : propertyDefinitions) {
			propertyVersionInsertQuery = propertyVersionInsertQuery.values(artifactVersionId, id);
		}

		propertyVersionInsertQuery.execute();
	}

}
