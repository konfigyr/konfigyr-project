package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Slug;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class ServicesTest extends AbstractIntegrationTest {

	@Autowired
	NamespaceManager namespaces;

	@Autowired
	Services services;

	@Test
	@DisplayName("should search services for john-doe namespace")
	void shouldFindNamespaces() {
		final var namespace = namespaces.findById(EntityId.from(1)).orElseThrow();

		final var query = SearchQuery.builder()
				.build();

		assertThat(services.find(namespace, query))
				.isNotNull()
				.hasSize(1)
				.extracting(Service::id)
				.containsExactlyInAnyOrder(EntityId.from(1));
	}

	@Test
	@DisplayName("should search services for konfigyr namespace with search term")
	void shouldSearchNamespacesBySearchTerm() {
		final var namespace = namespaces.findById(EntityId.from(2)).orElseThrow();

		final var query = SearchQuery.builder()
				.term("rest api")
				.build();

		assertThat(services.find(namespace, query))
				.isNotNull()
				.hasSize(1)
				.extracting(Service::id)
				.containsExactlyInAnyOrder(EntityId.from(3));
	}

	@Test
	@DisplayName("should lookup service by entity identifier")
	void shouldLookupServiceById() {
		final var id = EntityId.from(1);

		assertThat(services.get(id))
				.isPresent()
				.get()
				.returns(id, Service::id)
				.returns("john-doe-blog", Service::slug)
				.returns("John Doe Blog", Service::name)
				.returns("Personal John Doe blog application", Service::description)
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
	@DisplayName("should lookup unknown service by entity identifier")
	void shouldLookupUnknownServiceById() {
		assertThat(services.get(EntityId.from(9999)))
				.isEmpty();
	}

	@Test
	@DisplayName("should lookup service by namespace and slug")
	void shouldLookupServiceBySlug() {
		final var namespace = namespaces.findById(EntityId.from(2)).orElseThrow();

		assertThat(services.get(namespace, "konfigyr-id"))
				.isPresent()
				.get()
				.returns(EntityId.from(2), Service::id)
				.returns("konfigyr-id", Service::slug)
				.returns("Konfigyr ID", Service::name)
				.returns("Konfigyr Identity service", Service::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should lookup service by slug that belongs to different namespace")
	void shouldLookupOtherServiceBySlug() {
		final var namespace = namespaces.findById(EntityId.from(1)).orElseThrow();

		assertThat(services.get(namespace, "konfigyr-id"))
				.isEmpty();
	}

	@Test
	@DisplayName("should lookup unknown service by namespace and slug")
	void shouldLookupUnknownServiceBySlug() {
		final var namespace = namespaces.findById(EntityId.from(2)).orElseThrow();

		assertThat(services.get(namespace, "unknown"))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should create service from definition")
	void shouldCreateService(AssertablePublishedEvents events) {
		final var definition = ServiceDefinition.builder()
				.namespace(2)
				.slug("konfigyr-registry")
				.name("Konfigyr registry")
				.build();

		final var service = services.create(definition);
		assertThat(service.id())
				.isNotNull();

		assertThat(service)
				.returns(definition.namespace(), Service::namespace)
				.returns(definition.slug().get(), Service::slug)
				.returns(definition.name(), Service::name)
				.returns(definition.description(), Service::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(ServiceEvent.Created.class)
				.matching(EntityEvent::id, service.id());
	}

	@Test
	@DisplayName("should fail to create service for unknown namespace")
	void shouldCreateServiceForUnknownNamespace(AssertablePublishedEvents events) {
		final var definition = ServiceDefinition.builder()
				.namespace(9999)
				.slug("konfigyr-registry")
				.name("Konfigyr registry")
				.build();

		assertThatExceptionOfType(NamespaceNotFoundException.class)
				.isThrownBy(() -> services.create(definition))
				.withNoCause();

		assertThat(events.eventOfTypeWasPublished(ServiceEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to create service for already used slug")
	void shouldCreateServiceWithUsedSlug(AssertablePublishedEvents events) {
		final var definition = ServiceDefinition.builder()
				.namespace(2)
				.slug("konfigyr-id")
				.name("Konfigyr ID")
				.build();

		assertThatExceptionOfType(ServiceExistsException.class)
				.isThrownBy(() -> services.create(definition))
				.withCauseInstanceOf(DuplicateKeyException.class);

		assertThat(events.eventOfTypeWasPublished(ServiceEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should update service by identifier")
	void shouldUpdateService(AssertablePublishedEvents events) {
		final var definition = ServiceDefinition.builder()
				.namespace(2)
				.slug("konfigyr-id")
				.name("Konfigyr OIDC")
				.description("Konfigyr authorization server")
				.build();

		assertThat(services.update(EntityId.from(2), definition))
				.returns(EntityId.from(2), Service::id)
				.returns(definition.namespace(), Service::namespace)
				.returns(definition.slug().get(), Service::slug)
				.returns(definition.name(), Service::name)
				.returns(definition.description(), Service::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		assertThat(events.eventOfTypeWasPublished(ServiceEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should rename service by identifier")
	void shouldRenameService(AssertablePublishedEvents events) {
		final var definition = ServiceDefinition.builder()
				.namespace(2)
				.slug("konfigyr-rest-api")
				.name("Konfigyr API")
				.description(null)
				.build();

		assertThat(services.update(EntityId.from(3), definition))
				.returns(EntityId.from(3), Service::id)
				.returns(definition.namespace(), Service::namespace)
				.returns(definition.slug().get(), Service::slug)
				.returns(definition.name(), Service::name)
				.returns(definition.description(), Service::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(2), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(ServiceEvent.Renamed.class)
				.matching(EntityEvent::id, EntityId.from(3))
				.matching(ServiceEvent.Renamed::from, Slug.slugify("konfigyr-api"))
				.matching(ServiceEvent.Renamed::to, definition.slug());
	}

	@Test
	@DisplayName("should fail to update service with an existing slug")
	void shouldUpdateServiceWithExistingSlug(AssertablePublishedEvents events) {
		final var definition = ServiceDefinition.builder()
				.namespace(2)
				.slug("konfigyr-api")
				.name("Konfigyr REST API")
				.build();

		assertThatExceptionOfType(ServiceExistsException.class)
				.isThrownBy(() -> services.update(EntityId.from(2), definition))
				.withCauseInstanceOf(DuplicateKeyException.class);

		assertThat(events.eventOfTypeWasPublished(ServiceEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to update unknown service")
	void shouldUpdateUnknownService(AssertablePublishedEvents events) {
		final var definition = ServiceDefinition.builder()
				.namespace(2)
				.slug("konfigyr-id")
				.name("Konfigyr OIDC")
				.build();

		assertThatExceptionOfType(ServiceNotFoundException.class)
				.isThrownBy(() -> services.update(EntityId.from(9999), definition))
				.withNoCause();

		assertThat(events.eventOfTypeWasPublished(ServiceEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should delete service by identifier")
	void shouldDeleteServiceById(AssertablePublishedEvents events) {
		final var id = EntityId.from(1);

		assertThatNoException().isThrownBy(() -> services.delete(id));

		assertThat(services.get(id))
				.isEmpty();

		events.assertThat()
				.contains(ServiceEvent.Deleted.class)
				.matching(EntityEvent::id, id);
	}

	@Test
	@DisplayName("should fail to delete unknown service by identifier")
	void shouldDeleteUnknownServiceById(AssertablePublishedEvents events) {
		assertThatExceptionOfType(ServiceNotFoundException.class)
				.isThrownBy(() -> services.delete(EntityId.from(9999)))
				.withNoCause();

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should delete service by slug")
	void shouldDeleteServiceBySlug(AssertablePublishedEvents events) {
		final var namespace = namespaces.findById(EntityId.from(2)).orElseThrow();

		assertThatNoException().isThrownBy(() -> services.delete(namespace, "konfigyr-api"));

		assertThat(services.get(namespace, "konfigyr-api"))
				.isEmpty();

		events.assertThat()
				.contains(ServiceEvent.Deleted.class)
				.matching(EntityEvent::id, EntityId.from(3));
	}

	@Test
	@DisplayName("should fail to delete by slug that belongs to different namespace")
	void shouldDeleteOtherServiceBySlug(AssertablePublishedEvents events) {
		final var namespace = namespaces.findById(EntityId.from(2)).orElseThrow();

		assertThatExceptionOfType(ServiceNotFoundException.class)
				.isThrownBy(() -> services.delete(namespace, "john-doe-blog"))
				.withNoCause();

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to delete unknown service by slug")
	void shouldDeleteUnknownServiceBySlug(AssertablePublishedEvents events) {
		final var namespace = namespaces.findById(EntityId.from(2)).orElseThrow();

		assertThatExceptionOfType(ServiceNotFoundException.class)
				.isThrownBy(() -> services.delete(namespace, "unknown"))
				.withNoCause();

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

}
