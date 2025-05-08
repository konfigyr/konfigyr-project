package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.FullName;
import com.konfigyr.test.AbstractIntegrationTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class AccountManagerTest extends AbstractIntegrationTest {

	@Autowired
	AccountManager manager;

	@Test
	@DisplayName("should lookup account by entity identifier")
	void shouldLookupAccountById() {
		final var id = EntityId.from(1);

		assertThat(manager.findById(id))
				.isPresent()
				.get()
				.returns(id, Account::id)
				.returns(AccountStatus.ACTIVE, Account::status)
				.returns("john.doe@konfigyr.com", Account::email)
				.returns("John", Account::firstName)
				.returns("Doe", Account::lastName)
				.returns("John Doe", Account::displayName)
				.returns(FullName.of("John", "Doe"), Account::fullName)
				.returns(Avatar.generate(id, "JD"), Account::avatar)
				.returns(false, Account::isDeletable)
				.satisfies(it -> assertThat(it.lastLoginAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), byLessThan(10, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.updatedAt()).isNotNull())
				.satisfies(it -> assertThat(it.memberships())
						.hasSize(2)
						.extracting(Membership::id, Membership::namespace, Membership::role)
						.containsExactly(
								tuple(EntityId.from(1), "john-doe", NamespaceRole.ADMIN),
								tuple(EntityId.from(2), "konfigyr", NamespaceRole.ADMIN)
						)
				);
	}

	@Test
	@DisplayName("should lookup account by email address")
	void shouldLookupAccountByEmail() {
		final var email = "jane.doe@konfigyr.com";

		assertThat(manager.findByEmail(email))
				.isPresent()
				.get()
				.returns(EntityId.from(2), Account::id)
				.returns(AccountStatus.ACTIVE, Account::status)
				.returns(email, Account::email)
				.returns("Jane", Account::firstName)
				.returns("Doe", Account::lastName)
				.returns("Jane Doe", Account::displayName)
				.returns(FullName.of("Jane", "Doe"), Account::fullName)
				.returns(Avatar.generate(EntityId.from(2), "JD"), Account::avatar)
				.returns(true, Account::isDeletable)
				.returns(null, Account::lastLoginAt)
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.updatedAt()).isNotNull())
				.satisfies(it -> assertThat(it.memberships())
						.hasSize(1)
						.extracting(Membership::id, Membership::namespace, Membership::role)
						.containsExactly(
								tuple(EntityId.from(3), "konfigyr", NamespaceRole.USER)
						)
				);
	}

	@Test
	@DisplayName("should lookup account memberships")
	void shouldLookupAccountMemberships() {
		assertThat(manager.findMemberships(EntityId.from(2)))
				.hasSize(1)
				.first()
				.returns(EntityId.from(3), Membership::id)
				.returns("konfigyr", Membership::namespace)
				.returns(NamespaceRole.USER, Membership::role)
				.returns("Konfigyr", Membership::name)
				.returns(Avatar.generate("konfigyr", "K"), Membership::avatar)
				.satisfies(it -> assertThat(it.since())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(2), within(2, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should return empty optional when account is not found by entity identifier")
	void shouldFailToLookupAccountById() {
		assertThat(manager.findById(EntityId.from(991827464))).isEmpty();
	}

	@Test
	@DisplayName("should return empty optional when account is not found by email address")
	void shouldFailToLookupAccountByEmail() {
		assertThat(manager.findByEmail("unknown@konfigyr.com")).isEmpty();
	}

	@Test
	@DisplayName("should throw account not found when fetching memberships for an unknown account")
	void shouldFailToLookupAccountMemberships() {
		assertThatThrownBy(() -> manager.findMemberships(EntityId.from(18365)))
				.isInstanceOf(AccountNotFoundException.class);
	}

	@Test
	@Transactional
	@DisplayName("should update account information")
	void shouldUpdateAccount(PublishedEvents events) {
		final var john = manager.findById(EntityId.from(1)).orElseThrow();
		final var updates = Account.builder(john)
				.firstName("Gurney")
				.lastName("Halleck")
				.avatar("https://example.com/gurney.svg")
				.build();

		assertThat(manager.update(updates))
				.returns(john.id(), Account::id)
				.returns(john.email(), Account::email)
				.returns(john.status(), Account::status)
				.returns(updates.firstName(), Account::firstName)
				.returns(updates.lastName(), Account::lastName)
				.returns(updates.avatar(), Account::avatar)
				.returns(john.memberships(), Account::memberships)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isNotEqualTo(john.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(400, ChronoUnit.MILLIS))
				);

		events.eventOfTypeWasPublished(AccountEvent.Updated.class);
	}

	@Test
	@Transactional
	@DisplayName("should fail to update account due to database constraint violation")
	void shouldFailToUpdateAccount(PublishedEvents events) {
		final var john = manager.findById(EntityId.from(1)).orElseThrow();
		final var updates = Account.builder(john)
				.firstName(RandomStringUtils.randomAlphanumeric(512))
				.build();

		assertThatThrownBy(() -> manager.update(updates))
				.isInstanceOf(AccountException.class)
				.hasCauseInstanceOf(DataIntegrityViolationException.class);

		assertThat(events.ofType(AccountEvent.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should fail to update account that does not exist")
	void shouldNotUpdateAccountThatDoesNotExist(PublishedEvents events) {
		final var updates = Account.builder()
				.id(1248765124L)
				.status(AccountStatus.ACTIVE)
				.email("gurney.halleck@atreides.com")
				.build();

		assertThatThrownBy(() -> manager.update(updates))
				.isInstanceOf(AccountNotFoundException.class);

		assertThat(events.ofType(AccountEvent.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should delete account when he is no longer an admin member")
	void shouldDeleteAccount(PublishedEvents events) {
		assertThatNoException().isThrownBy(() -> manager.delete(EntityId.from(2)));

		events.eventOfTypeWasPublished(AccountEvent.Deleted.class);

		assertThat(manager.findById(EntityId.from(2)))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to delete account when he is an admin member")
	void shouldNotDeleteAccountWithAdminMemberships(PublishedEvents events) {
		assertThatThrownBy(() -> manager.delete(EntityId.from(1)))
				.isInstanceOf(AccountException.class)
				.hasMessageContaining("Can not delete account that is still an admin of non-personal namespaces")
				.hasNoCause();

		assertThat(events.ofType(AccountEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to delete account that does not exist")
	void shouldFailToDeleteUnknownAccount(PublishedEvents events) {
		assertThatThrownBy(() -> manager.delete(EntityId.from(1247811)))
				.isInstanceOf(AccountNotFoundException.class)
				.hasNoCause();

		assertThat(events.ofType(AccountEvent.class))
				.isEmpty();
	}

}
