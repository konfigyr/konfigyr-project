package com.konfigyr.account;

import com.konfigyr.NamespaceTestConfiguration;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.namespace.NamespaceType;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.FullName;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest(classes = NamespaceTestConfiguration.class)
@ExtendWith(PublishedEventsExtension.class)
class AccountManagerTest {

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
						.extracting(Membership::id, Membership::namespace, Membership::type, Membership::role)
						.containsExactly(
								tuple(EntityId.from(1), "john-doe", NamespaceType.PERSONAL, NamespaceRole.ADMIN),
								tuple(EntityId.from(2), "konfigyr", NamespaceType.TEAM, NamespaceRole.ADMIN)
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
						.extracting(Membership::id, Membership::namespace, Membership::type, Membership::role)
						.containsExactly(
								tuple(EntityId.from(3), "konfigyr", NamespaceType.TEAM, NamespaceRole.USER)
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
				.returns(NamespaceType.TEAM, Membership::type)
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
	@DisplayName("should create account with registration data with an avatar")
	void shouldRegisterAccount() {
		final var registration = AccountRegistration.builder()
				.email("muad.dib@arakis.com")
				.firstName("Paul")
				.lastName("Atreides")
				.avatar("https://i.pinimg.com/originals/53/32/87/53328770473d527d3400355e9d0edb15.jpg")
				.build();

		assertThat(manager.create(registration))
				.returns(AccountStatus.ACTIVE, Account::status)
				.returns("muad.dib@arakis.com", Account::email)
				.returns("Paul", Account::firstName)
				.returns("Atreides", Account::lastName)
				.returns("Paul Atreides", Account::displayName)
				.returns(FullName.of("Paul", "Atreides"), Account::fullName)
				.returns(Avatar.parse("https://i.pinimg.com/originals/53/32/87/53328770473d527d3400355e9d0edb15.jpg"), Account::avatar)
				.returns(null, Account::lastLoginAt)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.updatedAt()).isNotNull());
	}

	@Test
	@Transactional
	@DisplayName("should create account with registration data with a generated avatar")
	void shouldRegisterAccountWithGeneratedAvatar(PublishedEvents events) {
		final var registration = AccountRegistration.builder()
				.email("muad.dib@arakis.com")
				.fullName("Paul Atreides")
				.build();

		final var account = manager.create(registration);

		assertThat(account)
				.returns(AccountStatus.ACTIVE, Account::status)
				.returns("muad.dib@arakis.com", Account::email)
				.returns("Paul", Account::firstName)
				.returns("Atreides", Account::lastName)
				.returns("Paul Atreides", Account::displayName)
				.returns(FullName.of("Paul", "Atreides"), Account::fullName)
				.returns(Avatar.generate(account.id(), "PA"), Account::avatar)
				.returns(Memberships.empty(), Account::memberships)
				.returns(null, Account::lastLoginAt)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.updatedAt()).isNotNull());

		events.eventOfTypeWasPublished(AccountEvent.Registered.class);
	}

	@Test
	@DisplayName("should fail to create account with existing email address")
	void shouldNotRegisterAccountWithSameEmail(PublishedEvents events) {
		final var registration = AccountRegistration.builder()
				.email("jane.doe@konfigyr.com")
				.fullName("Jane Doe")
				.build();

		assertThatThrownBy(() -> manager.create(registration))
				.isInstanceOf(AccountExistsException.class)
				.hasCauseInstanceOf(DuplicateKeyException.class);

		assertThat(events.ofType(AccountEvent.class))
				.isEmpty();
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