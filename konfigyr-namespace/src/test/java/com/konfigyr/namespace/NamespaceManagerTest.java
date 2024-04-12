package com.konfigyr.namespace;

import com.konfigyr.NamespaceTestConfiguration;
import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = NamespaceTestConfiguration.class)
@ExtendWith(PublishedEventsExtension.class)
class NamespaceManagerTest {

	@Autowired
	NamespaceManager manager;

	@Test
	@DisplayName("should lookup namespace by entity identifier")
	void shouldLookupNamespaceById() {
		final var id = EntityId.from(1);

		assertThat(manager.findById(id))
				.isPresent()
				.get()
				.returns(id, Namespace::id)
				.returns("john-doe", Namespace::slug)
				.returns(NamespaceType.PERSONAL, Namespace::type)
				.returns("John Doe", Namespace::name)
				.returns("Personal namespace for John Doe", Namespace::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now().minusDays(5))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now().minusDays(1))
				);
	}

	@Test
	@DisplayName("should lookup namespace by slug path")
	void shouldLookupNamespaceBySlug() {
		final var slug = "konfigyr";

		assertThat(manager.findBySlug(slug))
				.isPresent()
				.get()
				.returns(EntityId.from(2), Namespace::id)
				.returns(slug, Namespace::slug)
				.returns(NamespaceType.TEAM, Namespace::type)
				.returns("Konfigyr", Namespace::name)
				.returns("Konfigyr namespace", Namespace::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now().minusDays(3))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now().minusDays(1))
				);
	}

	@Test
	@DisplayName("should check if namespaces exists by slug path")
	void shouldCheckIfNamespaceExists() {
		assertThat(manager.exists("john-doe")).isTrue();
		assertThat(manager.exists("konfigyr")).isTrue();
		assertThat(manager.exists("unknown")).isFalse();
	}

	@Test
	@DisplayName("should return empty optional when namespace is not found by entity identifier")
	void shouldFailToLookupNamespaceById() {
		assertThat(manager.findById(EntityId.from(991827464))).isEmpty();
	}

	@Test
	@DisplayName("should return empty optional when namespace is not found by path slug")
	void shouldFailToLookupNamespaceByEmail() {
		assertThat(manager.findBySlug("unknown")).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should create namespace for definition")
	void shouldCreateNamespace(PublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.type(NamespaceType.ENTERPRISE)
				.slug("arakis")
				.name("Arakis")
				.description("Harsh desert planet located in the Canopus star system")
				.build();

		assertThat(manager.create(definition))
				.returns(NamespaceType.ENTERPRISE, Namespace::type)
				.returns("arakis", Namespace::slug)
				.returns("Arakis", Namespace::name)
				.returns("Harsh desert planet located in the Canopus star system", Namespace::description)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.updatedAt()).isNotNull());

		events.eventOfTypeWasPublished(NamespaceEvent.Created.class);
	}

	@Test
	@DisplayName("should fail to create namespace with unknown owner")
	void shouldNotCreateNamespaceWithUnknownOwner(PublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(EntityId.from(999999).serialize())
				.type(NamespaceType.ENTERPRISE)
				.slug("arakis")
				.name("Arakis")
				.build();

		assertThatThrownBy(() -> manager.create(definition))
				.isInstanceOf(NamespaceOwnerException.class)
				.hasCauseInstanceOf(DataIntegrityViolationException.class)
				.extracting("definition", "owner")
				.containsExactly(definition, definition.owner());

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to create namespace with existing slug")
	void shouldNotCreateNamespaceWithExistingSlug(PublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.type(NamespaceType.TEAM)
				.slug("konfigyr")
				.name("Konfigyr")
				.build();

		assertThatThrownBy(() -> manager.create(definition))
				.isInstanceOf(NamespaceExistsException.class)
				.hasCauseInstanceOf(DuplicateKeyException.class)
				.extracting("definition")
				.isEqualTo(definition);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

}