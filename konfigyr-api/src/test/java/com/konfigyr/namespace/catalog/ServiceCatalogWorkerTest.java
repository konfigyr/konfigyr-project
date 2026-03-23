package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.ServiceCatalog;
import com.konfigyr.namespace.Services;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

class ServiceCatalogWorkerTest extends AbstractIntegrationTest {

	@Autowired
	Services services;

	@Autowired
	ServiceCatalogWorker worker;

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
		assertThatNoException().isThrownBy(() -> worker.build(EntityId.from(2)));

		assertThat(services.catalog(EntityId.from(3)))
				.hasSize(5)
				.satisfiesExactly(
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
		assertThatNoException().isThrownBy(() -> worker.build(EntityId.from(1)));

		assertThat(services.catalog(EntityId.from(2)))
				.hasSize(10)
				.satisfiesExactly(
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

}
