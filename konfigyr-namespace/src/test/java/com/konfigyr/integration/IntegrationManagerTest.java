package com.konfigyr.integration;

import com.konfigyr.NamespaceTestConfiguration;
import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.modulith.test.PublishedEventsExtension;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest(classes = NamespaceTestConfiguration.class)
@ExtendWith(PublishedEventsExtension.class)
class IntegrationManagerTest {

	@Autowired
	IntegrationManager manager;

	@Test
	@DisplayName("should lookup integrations for namespace")
	void shouldLookupIntegrationsForNamespace() {
		final var id = EntityId.from(1);

		assertThat(manager.find(id, Pageable.unpaged()))
				.hasSize(1)
				.first()
				.returns(EntityId.from(1), Integration::id)
				.returns(id, Integration::namespace)
				.returns(IntegrationType.SOURCE_CODE, Integration::type)
				.returns(IntegrationProvider.GITHUB, Integration::provider)
				.returns("110011", Integration::reference)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(5), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should lookup an integration for namespace")
	void shouldLookupIntegrationForNamespace() {
		final var id = EntityId.from(1);

		assertThat(manager.get("konfigyr", EntityId.from(2)))
				.isPresent()
				.get()
				.returns(EntityId.from(2), Integration::id)
				.returns(EntityId.from(2), Integration::namespace)
				.returns(IntegrationType.SOURCE_CODE, Integration::type)
				.returns(IntegrationProvider.GITHUB, Integration::provider)
				.returns("220022", Integration::reference)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should fail to lookup integrations for unknown namespace")
	void shouldReturnEmptyPageForUnknownNamespace() {
		assertThat(manager.find("unknown", Pageable.unpaged()))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to lookup integration for unknown namespace")
	void shouldReturnEmptyIntegrationForUnknownNamespace() {
		assertThat(manager.get("unknown", EntityId.from(1)))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to lookup integration that does not belong to a namespace")
	void shouldReturnEmptyIntegration() {
		assertThat(manager.get(EntityId.from(2), EntityId.from(1)))
				.isEmpty();
	}
}
