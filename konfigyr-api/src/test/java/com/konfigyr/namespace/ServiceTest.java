package com.konfigyr.namespace;

import com.konfigyr.artifactory.BooleanSchema;
import com.konfigyr.artifactory.StringSchema;
import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceTest {

	@Test
	@DisplayName("should create service using fluent builder")
	void shouldCreateService() {
		final var service = Service.builder()
				.id(836571L)
				.namespace(8561253L)
				.slug("test-service")
				.name("Test service")
				.description("My testing service")
				.createdAt(Instant.now().minus(62, ChronoUnit.DAYS))
				.updatedAt(Instant.now().minus(16, ChronoUnit.HOURS))
				.build();

		assertThat(service)
				.returns(EntityId.from(836571L), Service::id)
				.returns(EntityId.from(8561253L), Service::namespace)
				.returns("test-service", Service::slug)
				.returns("Test service", Service::name)
				.returns("My testing service", Service::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(ZoneOffset.UTC).minusDays(62), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(ZoneOffset.UTC).minusHours(16), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should validate service data when using fluent builder")
	void shouldValidateServiceBuilder() {
		final var builder = Service.builder();

		assertThatThrownBy(builder::build)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Service entity identifier can't be null");

		assertThatThrownBy(() -> builder.id("000000BKTH3TG").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace entity identifier can't be null");

		assertThatThrownBy(() -> builder.namespace("000000BKTH3TG").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Service slug can't be blank");

		assertThatThrownBy(() -> builder.slug("test-service").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Service name can't be blank");

		assertThat(builder.name("Service name").build())
				.returns(EntityId.from(12476518224L), Service::id)
				.returns(EntityId.from(12476518224L), Service::namespace)
				.returns("test-service", Service::slug)
				.returns("Service name", Service::name)
				.returns(null, Service::description)
				.returns(null, Service::createdAt)
				.returns(null, Service::updatedAt);
	}

	@Test
	@DisplayName("should create service catalog")
	void shouldCreateCatalog() {
		final var properties = List.of(
				ServiceCatalog.Property.builder()
						.groupId("org.springframework.boot")
						.artifactId("spring-boot-autoconfigure")
						.version("4.0.4")
						.name("spring.task.execution.mode")
						.typeName("org.springframework.boot.autoconfigure.task.TaskExecutionProperties$Mode")
						.schema(StringSchema.builder().enumeration("AUTO").enumeration("FORCE").build())
						.description("Determine when the task executor is to be created.")
						.defaultValue("AUTO")
						.build(),
				ServiceCatalog.Property.builder()
						.groupId("org.springframework.boot")
						.artifactId("spring-boot-autoconfigure")
						.version("4.0.4")
						.name("spring.task.execution.pool.allow-core-thread-timeout")
						.typeName("java.lang.Boolean")
						.schema(BooleanSchema.instance())
						.description("Whether core threads are allowed to time out. This enables dynamic growing and shrinking of the pool. Doesn't have an effect if virtual threads are enabled.")
						.defaultValue("true")
						.build()
		);

		final var service = Service.builder()
				.id(1235L)
				.namespace(9146L)
				.slug("spring-service")
				.name("Spring service")
				.build();

		final var catalog = new ServiceCatalog(EntityId.from(5L), service, "1.0.0", properties);

		assertThatObject(catalog)
				.returns(EntityId.from(5L), ServiceCatalog::id)
				.returns(service, ServiceCatalog::service)
				.returns("1.0.0", ServiceCatalog::version)
				.returns(properties, ServiceCatalog::properties);

		assertThat(catalog.get("spring.task.execution.pool.allow-core-thread-timeout"))
				.hasValue(properties.get(1));

		assertThat(catalog.get("spring.task.execution.pool.core-size"))
				.isEmpty();
	}

}
