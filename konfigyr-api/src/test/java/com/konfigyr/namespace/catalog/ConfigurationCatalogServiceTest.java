package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.ServiceCatalog;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.namespace.Services;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static com.konfigyr.data.tables.ServiceConfigurationCatalog.SERVICE_CONFIGURATION_CATALOG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.awaitility.Awaitility.await;

class ConfigurationCatalogServiceTest extends AbstractIntegrationTest {

	@Autowired
	DSLContext context;

	@Autowired
	Services services;

	@Autowired
	ConfigurationCatalogService configurationCatalogService;

	@Test
	@DisplayName("should create and drop configuration catalog partition for service")
	void createAndDropServiceCatalogPartition() {
		final var service = services.get(EntityId.from(1)).orElseThrow();

		configurationCatalogService.createConfigurationCatalogPartition(new ServiceEvent.Created(service));
		await().untilAsserted(() -> assertPartitionExists(service.id(), true));

		configurationCatalogService.dropConfigurationCatalogPartition(new ServiceEvent.Deleted(service));
		await().untilAsserted(() -> assertPartitionExists(service.id(), false));
	}

	@Test
	@DisplayName("should search configuration catalog for a service without search term")
	void searchCatalogWithoutSearchTerm() {
		final var query = SearchQuery.of(Pageable.ofSize(2));
		final var service = services.get(EntityId.from(2)).orElseThrow();
		final var properties = configurationCatalogService.query(service, query);

		assertThatObject(properties)
				.returns(0, Page::getNumber)
				.returns(2, Page::getSize)
				.returns(2, Page::getNumberOfElements)
				.returns(3L, Page::getTotalElements)
				.returns(2, Page::getTotalPages);

		assertThat(properties.getContent())
				.hasSize(2)
				.containsExactly(
						ServiceCatalog.Property.builder()
								.coordinates(ArtifactCoordinates.of("org.springframework.boot", "spring-boot", "4.0.3"))
								.name("spring.application.deprecated")
								.typeName("java.lang.Boolean")
								.schema(BooleanSchema.instance())
								.description("Deprecated property that is no longer needed.")
								.defaultValue("true")
								.deprecation("No longer needed", null)
								.build(),
						ServiceCatalog.Property.builder()
								.coordinates(ArtifactCoordinates.of("org.springframework.boot", "spring-boot", "4.0.3"))
								.name("spring.application.index")
								.typeName("java.lang.Integer")
								.schema(IntegerSchema.builder().format("int32").build())
								.description("Application index.")
								.build()
				);
	}

	@Test
	@DisplayName("should search configuration catalog for a service matching description")
	void termShouldMatchDescription() {
		final var query = SearchQuery.builder()
				.term("no needed")
				.build();

		final var service = services.get(EntityId.from(2)).orElseThrow();
		final var properties = configurationCatalogService.query(service, query);

		assertThatObject(properties)
				.returns(0, Page::getNumber)
				.returns(20, Page::getSize)
				.returns(1, Page::getNumberOfElements)
				.returns(1L, Page::getTotalElements)
				.returns(1, Page::getTotalPages);

		assertThat(properties.getContent())
				.hasSize(1)
				.extracting(PropertyDescriptor::name)
				.containsExactly("spring.application.deprecated");
	}

	@Test
	@DisplayName("should search configuration catalog for a service matching name")
	void termShouldMatchName() {
		final var query = SearchQuery.builder()
				.term("spring.application.name")
				.build();

		final var service = services.get(EntityId.from(2)).orElseThrow();
		final var properties = configurationCatalogService.query(service, query);

		assertThatObject(properties)
				.returns(0, Page::getNumber)
				.returns(20, Page::getSize)
				.returns(1, Page::getNumberOfElements)
				.returns(1L, Page::getTotalElements)
				.returns(1, Page::getTotalPages);

		assertThat(properties.getContent())
				.hasSize(1)
				.containsExactly(
						ServiceCatalog.Property.builder()
								.coordinates(ArtifactCoordinates.of("org.springframework.boot", "spring-boot", "4.0.3"))
								.name("spring.application.name")
								.typeName("java.lang.String")
								.schema(StringSchema.instance())
								.description("Application name. Typically used with logging to help identify the application.")
								.build()
				);
	}

	@Test
	@DisplayName("should search configuration catalog with search term where no matches are found")
	void termShouldNotMatchAnyProperty() {
		final var query = SearchQuery.builder()
				.term("non-matching search term")
				.build();

		final var service = services.get(EntityId.from(2)).orElseThrow();

		assertThat(configurationCatalogService.query(service, query))
				.isEmpty();
	}

	private void assertPartitionExists(EntityId id, boolean expected) {
		final int count = context.select(DSL.count())
				.from("pg_inherits i")
				.join("pg_class child").on("i.inhrelid = child.oid")
				.join("pg_class parent").on("i.inhparent = parent.oid")
				.where("parent.relname = {0}", SERVICE_CONFIGURATION_CATALOG.getName()) // Parent table name
				.and("child.relname = {0}", SERVICE_CONFIGURATION_CATALOG.getName() + '_' + id.get())
				.fetchOptional(Record1::component1)
				.orElse(0);

		assertThat(count).isEqualTo(expected ? 1 : 0);
	}

}
