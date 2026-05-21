package com.konfigyr.membership;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceEvent;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

class MembershipsTest extends AbstractIntegrationTest {

	@Autowired
	NamespaceManager namespaces;

	@Autowired
	Memberships members;

	@Test
	@DisplayName("should lookup namespace members by entity identifier")
	void shouldLookupNamespaceMembersById() {
		final var id = EntityId.from(1);

		assertThat(members.find(lookupNamespace("john-doe")))
				.isNotNull()
				.hasSize(1)
				.extracting(Member::id, Member::namespace, Member::account, Member::role, Member::email)
				.containsExactlyInAnyOrder(
						tuple(EntityId.from(1), id, EntityId.from(1), NamespaceRole.ADMIN, "john.doe@konfigyr.com")
				);
	}

	@Test
	@DisplayName("should lookup namespace members by slug path")
	void shouldLookupNamespaceMembersBySlug() {
		assertThat(members.find(lookupNamespace("konfigyr")))
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

		assertThat(members.find(lookupNamespace("konfigyr"), query))
				.isNotNull()
				.hasSize(1)
				.extracting(Member::id)
				.containsExactlyInAnyOrder(EntityId.from(2));
	}

	@Test
	@DisplayName("should retrieve namespace member")
	void shouldRetrieveNamespaceMember() {
		assertThat(members.get(lookupNamespace("konfigyr"), EntityId.from(2)))
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
		assertThat(members.get(lookupNamespace("konfigyr"), EntityId.from(9999)))
				.isNotNull()
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should update namespace member")
	void shouldUpdateNamespaceMember(AssertablePublishedEvents events) {
		assertThat(members.update(lookupNamespace("konfigyr"), EntityId.from(3), NamespaceRole.ADMIN))
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
		assertThatThrownBy(() -> members.update(lookupNamespace("konfigyr"), EntityId.from(2), NamespaceRole.USER))
				.isInstanceOf(UnsupportedMembershipOperationException.class);

		assertThat(events.ofType(NamespaceEvent.MemberUpdated.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to update unknown namespace member")
	void shouldFailToUpdateUnknownNamespaceMember(AssertablePublishedEvents events) {
		assertThatThrownBy(() -> members.update(lookupNamespace("john-doe"), EntityId.from(9999), NamespaceRole.USER))
				.isInstanceOf(MemberNotFoundException.class);

		assertThat(events.ofType(NamespaceEvent.MemberUpdated.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should remove namespace member")
	void shouldRemoveNamespaceMember(AssertablePublishedEvents events) {
		assertThatNoException().isThrownBy(() -> members.remove(lookupNamespace("konfigyr"), EntityId.from(3)));

		assertThat(members.find(lookupNamespace("konfigyr")))
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
		assertThatThrownBy(() -> members.remove(lookupNamespace("konfigyr"), EntityId.from(2)))
				.isInstanceOf(UnsupportedMembershipOperationException.class);

		assertThat(events.ofType(NamespaceEvent.MemberRemoved.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should remove namespace member that does not exist")
	void shouldRemoveNonExistingNamespaceMember(AssertablePublishedEvents events) {
		assertThatThrownBy(() -> members.remove(lookupNamespace("konfigyr"), EntityId.from(9999)))
				.isInstanceOf(MemberNotFoundException.class);

		assertThat(members.find(lookupNamespace("konfigyr")))
				.isNotNull()
				.hasSize(2)
				.extracting(Member::id)
				.containsExactlyInAnyOrder(EntityId.from(2), EntityId.from(3));

		assertThat(events.ofType(NamespaceEvent.MemberRemoved.class))
				.isEmpty();
	}

	private Namespace lookupNamespace(String slug) {
		return assertThat(namespaces.findBySlug(slug))
				.as("Namespace with slug '%s' not found", slug)
				.isPresent()
				.get()
				.actual();
	}

}
