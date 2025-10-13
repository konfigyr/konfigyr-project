package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Avatar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

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
				.hasMessageContaining("Service entity identifier can not be null");

		assertThatThrownBy(() -> builder.id("000000BKTH3TG").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace entity identifier can not be null");

		assertThatThrownBy(() -> builder.namespace("000000BKTH3TG").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Service slug can not be blank");

		assertThatThrownBy(() -> builder.slug("test-service").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Service name can not be blank");

		assertThat(builder.name("Service name").build())
				.returns(EntityId.from(12476518224L), Service::id)
				.returns(EntityId.from(12476518224L), Service::namespace)
				.returns("test-service", Service::slug)
				.returns("Service name", Service::name)
				.returns(null, Service::description)
				.returns(null, Service::createdAt)
				.returns(null, Service::updatedAt);
	}

}
