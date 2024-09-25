package com.konfigyr.namespace;

import com.konfigyr.NamespaceTestConfiguration;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.SearchQuery;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

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
						.isCloseTo(OffsetDateTime.now().minusDays(5), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should lookup namespace members by entity identifier")
	void shouldLookupNamespaceMembersById() {
		final var id = EntityId.from(1);

		assertThat(manager.findMembers(id))
				.isNotNull()
				.hasSize(1)
				.extracting(Member::id, Member::namespace, Member::account, Member::role, Member::email)
				.containsExactlyInAnyOrder(
						tuple(EntityId.from(1), id, EntityId.from(1), NamespaceRole.ADMIN, "john.doe@konfigyr.com")
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
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should lookup namespace members by slug path")
	void shouldLookupNamespaceMembersBySlug() {
		final var slug = "konfigyr";

		assertThat(manager.findMembers(slug))
				.isNotNull()
				.hasSize(2)
				.extracting(Member::id, Member::namespace, Member::account, Member::role, Member::email)
				.containsExactlyInAnyOrder(
						tuple(EntityId.from(2), EntityId.from(2), EntityId.from(1), NamespaceRole.ADMIN, "john.doe@konfigyr.com"),
						tuple(EntityId.from(3), EntityId.from(2), EntityId.from(2), NamespaceRole.USER, "jane.doe@konfigyr.com")
				);
	}

	@Test
	@DisplayName("should search namespace members")
	void shouldSearchMembers() {
		final var query = SearchQuery.builder()
				.term("John")
				.build();

		assertThat(manager.findMembers("konfigyr", query))
				.isNotNull()
				.hasSize(1)
				.extracting(Member::id)
				.containsExactlyInAnyOrder(EntityId.from(2));
	}

	@Test
	@Transactional
	@DisplayName("should update namespace member")
	void shouldUpdateNamespaceMember() {
		assertThat(manager.updateMember(EntityId.from(2), NamespaceRole.USER))
				.isNotNull()
				.returns(EntityId.from(2), Member::id)
				.returns(EntityId.from(1), Member::account)
				.returns(EntityId.from(2), Member::namespace)
				.returns(NamespaceRole.USER, Member::role);
	}

	@Test
	@Transactional
	@DisplayName("should remove namespace member")
	void shouldRemoveNamespaceMember() {
		assertThatNoException().isThrownBy(() -> manager.removeMember(EntityId.from(2)));

		assertThat(manager.findMembers("konfigyr"))
				.isNotNull()
				.hasSize(1)
				.extracting(Member::id)
				.containsExactly(EntityId.from(3));
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
	@DisplayName("should return empty members page when namespace is not found by entity identifier")
	void shouldFailToLookupNamespaceMembersById() {
		assertThat(manager.findMembers(EntityId.from(9914))).isEmpty();
	}

	@Test
	@DisplayName("should return empty members page when namespace is not found by path slug")
	void shouldFailToLookupNamespaceMembersByEmail() {
		assertThat(manager.findMembers("unknown")).isEmpty();
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

		final Namespace namespace = manager.create(definition);

		assertThat(namespace)
				.returns(NamespaceType.ENTERPRISE, Namespace::type)
				.returns("arakis", Namespace::slug)
				.returns("Arakis", Namespace::name)
				.returns("Harsh desert planet located in the Canopus star system", Namespace::description)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(400, ChronoUnit.MILLIS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(400, ChronoUnit.MILLIS))
				);

		events.eventOfTypeWasPublished(NamespaceEvent.Created.class);

		assertThat(manager.findMembers(namespace))
				.isNotNull()
				.hasSize(1)
				.first()
				.returns(namespace.id(), Member::namespace)
				.returns(definition.owner(), Member::account)
				.returns(NamespaceRole.ADMIN, Member::role)
				.returns("john.doe@konfigyr.com", Member::email)
				.returns("John Doe", Member::displayName)
				.returns(Avatar.generate(definition.owner(), "JD"), Member::avatar)
				.satisfies(it -> assertThat(it.since())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);
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
				.hasNoCause()
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

	@Test
	@DisplayName("should fail to create namespace when name is too long")
	void shouldNotCreateNamespaceWithLongNames(PublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.type(NamespaceType.PERSONAL)
				.slug("name-too-long")
				.name(RandomStringUtils.randomAlphanumeric(512))
				.build();

		assertThatThrownBy(() -> manager.create(definition))
				.isInstanceOf(NamespaceException.class)
				.hasCauseInstanceOf(DataAccessException.class);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to update unknown namespace member")
	void shouldFailToUpdateUnknownNamespaceMember() {
		assertThatThrownBy(() -> manager.updateMember(EntityId.from(9999), NamespaceRole.USER))
				.isInstanceOf(NamespaceException.class)
				.hasMessageContaining("Failed to update unknown member");
	}

}