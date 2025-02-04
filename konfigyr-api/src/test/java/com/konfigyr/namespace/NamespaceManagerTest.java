
package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Slug;
import com.konfigyr.test.AbstractIntegrationTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class NamespaceManagerTest extends AbstractIntegrationTest {

	@Autowired
	NamespaceManager manager;

	@Test
	@DisplayName("should search namespaces by search term")
	void shouldSearchNamespacesBySearchTerm() {
		final var query = SearchQuery.builder()
				.term("John")
				.build();

		assertThat(manager.search(query))
				.isNotNull()
				.hasSize(1)
				.extracting(Namespace::id)
				.containsExactlyInAnyOrder(EntityId.from(1));
	}

	@Test
	@DisplayName("should search namespaces by account access")
	void shouldSearchNamespacesByAccountAccess() {
		final var query = SearchQuery.builder()
				.criteria(SearchQuery.ACCOUNT, EntityId.from(1L))
				.build();

		assertThat(manager.search(query))
				.isNotNull()
				.hasSize(2)
				.extracting(Namespace::id)
				.containsExactlyInAnyOrder(
						EntityId.from(1),
						EntityId.from(2)
				);
	}

	@Test
	@DisplayName("should search namespaces by namespace slug")
	void shouldSearchNamespacesByNamespace() {
		final var query = SearchQuery.builder()
				.criteria(SearchQuery.NAMESPACE, "konfigyr")
				.build();

		assertThat(manager.search(query))
				.isNotNull()
				.hasSize(1)
				.extracting(Namespace::id)
				.containsExactlyInAnyOrder(
						EntityId.from(2)
				);
	}

	@Test
	@DisplayName("should search namespaces by account access and search term")
	void shouldSearchNamespacesByAccountAccessAndTerm() {
		final var query = SearchQuery.builder()
				.criteria(SearchQuery.ACCOUNT, EntityId.from(1L))
				.term("konf")
				.build();

		assertThat(manager.search(query))
				.isNotNull()
				.hasSize(1)
				.extracting(Namespace::id)
				.containsExactlyInAnyOrder(
						EntityId.from(2)
				);
	}

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
	@DisplayName("should retrieve namespace member")
	void shouldRetrieveNamespaceMember() {
		assertThat(manager.getMember(EntityId.from(2)))
				.isNotNull()
				.isNotEmpty()
				.get()
				.returns(EntityId.from(2), Member::id)
				.returns(EntityId.from(1), Member::account)
				.returns(EntityId.from(2), Member::namespace)
				.returns(NamespaceRole.ADMIN, Member::role);
	}

	@Test
	@DisplayName("should fail to retrieve unknown namespace member")
	void shouldRetrieveUnknownNamespaceMember() {
		assertThat(manager.getMember(EntityId.from(9999)))
				.isNotNull()
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should update namespace member")
	void shouldUpdateNamespaceMember(AssertablePublishedEvents events) {
		assertThat(manager.updateMember(EntityId.from(3), NamespaceRole.ADMIN))
				.isNotNull()
				.returns(EntityId.from(3), Member::id)
				.returns(EntityId.from(2), Member::account)
				.returns(EntityId.from(2), Member::namespace)
				.returns(NamespaceRole.ADMIN, Member::role);

		events.assertThat()
				.contains(NamespaceEvent.MemberUpdated.class)
				.matching(NamespaceEvent.MemberUpdated::id, EntityId.from(2))
				.matching(NamespaceEvent.MemberUpdated::account, EntityId.from(2))
				.matching(NamespaceEvent.MemberUpdated::role, NamespaceRole.ADMIN);
	}

	@Test
	@DisplayName("should fail to update last namespace admin member to user")
	void shouldUpdateLastAdministrator(AssertablePublishedEvents events) {
		assertThatThrownBy(() -> manager.updateMember(EntityId.from(2), NamespaceRole.USER))
				.isInstanceOf(UnsupportedMembershipOperationException.class);

		assertThat(events.ofType(NamespaceEvent.MemberUpdated.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to update unknown namespace member")
	void shouldFailToUpdateUnknownNamespaceMember(AssertablePublishedEvents events) {
		assertThatThrownBy(() -> manager.updateMember(EntityId.from(9999), NamespaceRole.USER))
				.isInstanceOf(MemberNotFoundException.class);

		assertThat(events.ofType(NamespaceEvent.MemberUpdated.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should remove namespace member")
	void shouldRemoveNamespaceMember(AssertablePublishedEvents events) {
		assertThatNoException().isThrownBy(() -> manager.removeMember(EntityId.from(3)));

		assertThat(manager.findMembers("konfigyr"))
				.isNotNull()
				.hasSize(1)
				.extracting(Member::id)
				.containsExactly(EntityId.from(2));

		events.assertThat()
				.contains(NamespaceEvent.MemberRemoved.class)
				.matching(NamespaceEvent.MemberRemoved::id, EntityId.from(2))
				.matching(NamespaceEvent.MemberRemoved::account, EntityId.from(2));
	}

	@Test
	@DisplayName("should not remove last namespace administrator member")
	void shouldRemoveLastAdministrator(AssertablePublishedEvents events) {
		assertThatThrownBy(() -> manager.removeMember(EntityId.from(2)))
				.isInstanceOf(UnsupportedMembershipOperationException.class);

		assertThat(events.ofType(NamespaceEvent.MemberRemoved.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should remove namespace member that does not exist")
	void shouldRemoveNonExistingNamespaceMember(AssertablePublishedEvents events) {
		assertThatThrownBy(() -> manager.removeMember(EntityId.from(9999)))
				.isInstanceOf(MemberNotFoundException.class);

		assertThat(manager.findMembers("konfigyr"))
				.isNotNull()
				.hasSize(2)
				.extracting(Member::id)
				.containsExactlyInAnyOrder(EntityId.from(2), EntityId.from(3));

		assertThat(events.ofType(NamespaceEvent.MemberRemoved.class))
				.isEmpty();
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
	void shouldCreateNamespace(AssertablePublishedEvents events) {
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

		events.assertThat()
				.contains(NamespaceEvent.Created.class)
				.matching(NamespaceEvent::id, namespace.id());

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
	void shouldNotCreateNamespaceWithUnknownOwner(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(EntityId.from(999999).serialize())
				.type(NamespaceType.ENTERPRISE)
				.slug("arakis")
				.name("Arakis")
				.build();

		assertThatThrownBy(() -> manager.create(definition))
				.isInstanceOf(NamespaceOwnerException.class)
				.hasNoCause()
				.asInstanceOf(InstanceOfAssertFactories.type(NamespaceOwnerException.class))
				.returns(definition, NamespaceOwnerException::getDefinition)
				.returns(definition.owner(), NamespaceOwnerException::getOwner);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to create namespace with existing slug")
	void shouldNotCreateNamespaceWithExistingSlug(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.type(NamespaceType.TEAM)
				.slug("konfigyr")
				.name("Konfigyr")
				.build();

		assertThatThrownBy(() -> manager.create(definition))
				.isInstanceOf(NamespaceExistsException.class)
				.hasCauseInstanceOf(DuplicateKeyException.class)
				.asInstanceOf(InstanceOfAssertFactories.type(NamespaceExistsException.class))
				.extracting(NamespaceExistsException::getDefinition)
				.isEqualTo(definition);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to create namespace when name is too long")
	void shouldNotCreateNamespaceWithLongNames(AssertablePublishedEvents events) {
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
	@Transactional
	@DisplayName("should update namespace")
	void shouldUpdateNamespace(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.type(NamespaceType.ENTERPRISE)
				.slug("konfigyr")
				.name("Konfigyr Namespace")
				.description("Updated description")
				.build();

		final Namespace namespace = manager.update("konfigyr", definition);

		assertThat(namespace)
				.returns(EntityId.from(2), Namespace::id)
				.returns(NamespaceType.TEAM, Namespace::type)
				.returns("konfigyr", Namespace::slug)
				.returns("Konfigyr Namespace", Namespace::name)
				.returns("Updated description", Namespace::description)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(400, ChronoUnit.MILLIS))
				);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should rename namespace")
	void shouldRenameNamespace(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.type(NamespaceType.ENTERPRISE)
				.slug("konfigyr-renamed")
				.name("Konfigyr Namespace")
				.build();

		final Namespace namespace = manager.update("konfigyr", definition);

		assertThat(namespace)
				.returns(EntityId.from(2), Namespace::id)
				.returns(NamespaceType.TEAM, Namespace::type)
				.returns("konfigyr-renamed", Namespace::slug)
				.returns("Konfigyr Namespace", Namespace::name)
				.returns(null, Namespace::description)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(400, ChronoUnit.MILLIS))
				);

		events.assertThat()
				.contains(NamespaceEvent.Renamed.class)
				.matching(NamespaceEvent::id, EntityId.from(2))
				.matching(NamespaceEvent.Renamed::from, Slug.slugify("konfigyr"))
				.matching(NamespaceEvent.Renamed::to, Slug.slugify("konfigyr-renamed"));
	}

	@Test
	@DisplayName("should fail to update unknown namespace")
	void shouldNotUpdateUnknownNamespace(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.type(NamespaceType.ENTERPRISE)
				.slug("konfigyr")
				.name("Konfigyr Namespace")
				.build();

		assertThatThrownBy(() -> manager.update("unknown", definition))
				.isInstanceOf(NamespaceNotFoundException.class)
				.hasNoCause();

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to update namespace with an already used slug")
	void shouldNotUpdateNamespaceWithExistingSlug(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.type(NamespaceType.ENTERPRISE)
				.slug("konfigyr")
				.name("Konfigyr Namespace")
				.build();

		assertThatThrownBy(() -> manager.update("john-doe", definition))
				.isInstanceOf(NamespaceExistsException.class)
				.hasCauseInstanceOf(DuplicateKeyException.class)
				.asInstanceOf(InstanceOfAssertFactories.type(NamespaceExistsException.class))
				.extracting(NamespaceExistsException::getDefinition)
				.isEqualTo(definition);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should delete namespace")
	void shouldDeleteNamespace(AssertablePublishedEvents events) {
		assertThatNoException().isThrownBy(() -> manager.delete("konfigyr"));

		assertThat(manager.findBySlug("konfigyr"))
				.isEmpty();

		events.assertThat()
				.contains(NamespaceEvent.Deleted.class)
				.matching(EntityEvent::id, EntityId.from(2));
	}

	@Test
	@DisplayName("should fail to delete unknown namespace")
	void shouldFailToDeleteUnknownNamespace(AssertablePublishedEvents events) {
		assertThatThrownBy(() -> manager.delete("unknown"))
				.isInstanceOf(NamespaceNotFoundException.class)
				.hasNoCause();

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

}
